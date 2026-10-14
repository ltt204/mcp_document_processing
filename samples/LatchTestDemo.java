import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

// Run with: java LatchTestDemo.java
public class LatchTestDemo {
    enum Status {
        PENDING, IN_PROGRESS, COMPLETED, ERROR
    }

    static class Job {
        final String id;
        final String path;
        volatile Status status = Status.PENDING;
        volatile String result;

        Job(String id, String path) {
            this.id = id;
            this.path = path;
        }
    }

    // Same as JobEngineDemo, but the parser is passed in so a test can fake it.
    static class JobService {
        private final Map<String, Job> jobs = new ConcurrentHashMap<>();
        private final ExecutorService pool;
        private final Function<String, String> parser;
        private int nextId = 1;

        JobService(ExecutorService pool, Function<String, String> parser) {
            this.pool = pool;
            this.parser = parser;
        }

        synchronized String submit(String path) {
            Job job = new Job("job-" + nextId++, path);
            jobs.put(job.id, job);
            try {
                pool.execute(() -> run(job));
            } catch (RejectedExecutionException e) {
                jobs.remove(job.id);
                throw new IllegalStateException("server busy, retry later");
            }
            return job.id;
        }

        private void run(Job job) {
            job.status = Status.IN_PROGRESS;
            try {
                job.result = parser.apply(job.path);
                job.status = Status.COMPLETED;
            } catch (Exception e) {
                job.result = e.getMessage();
                job.status = Status.ERROR;
            }
        }

        Job get(String id) {
            return jobs.get(id);
        }

        int size() {
            return jobs.size();
        }
    }

    static ExecutorService newPool() {
        return new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2));
    }

    // THE PROBLEM: read the status straight after submit, 1000 times.
    static void problem() {
        Map<Status, Integer> seen = new EnumMap<>(Status.class);
        for (int i = 0; i < 1000; i++) {
            ExecutorService pool = newPool();
            JobService service = new JobService(pool, path -> "text");
            String id = service.submit("a.pdf");
            seen.merge(service.get(id).status, 1, Integer::sum); // which state did we catch?
            pool.shutdown();
        }
        System.out.println("status right after submit, 1000 runs: " + seen);
    }

    // THE SOLUTION: the fake parser stops at a gate the test controls.
    static void solution() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(2); // workers -> test: "two of us are inside parse"
        CountDownLatch release = new CountDownLatch(1); // test -> workers: "you may finish now"

        Function<String, String> slowParser = path -> {
            started.countDown();
            try {
                release.await(); // the worker sleeps here until the test opens the gate
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "text of " + path;
        };

        ExecutorService pool = newPool();
        JobService service = new JobService(pool, slowParser);

        // 1. Two jobs fill the two threads.
        String a = service.submit("a.pdf");
        String b = service.submit("b.pdf");
        check(started.await(1, TimeUnit.SECONDS), "two workers reached parse()");
        check(service.get(a).status == Status.IN_PROGRESS, "a is IN_PROGRESS");
        check(service.get(b).status == Status.IN_PROGRESS, "b is IN_PROGRESS");

        // 2. Two more fill the queue. They cannot start: both threads are held at the gate.
        String c = service.submit("c.pdf");
        String d = service.submit("d.pdf");
        check(service.get(c).status == Status.PENDING, "c is PENDING");
        check(service.get(d).status == Status.PENDING, "d is PENDING");

        // 3. The fifth is refused and leaves no record.
        try {
            service.submit("e.pdf");
            check(false, "fifth submit should have been refused");
        } catch (IllegalStateException expected) {
            check(service.size() == 4, "refused job is not stored");
        }

        // 4. Open the gate, wait for the pool to drain, check the results.
        release.countDown();
        pool.shutdown();
        check(pool.awaitTermination(1, TimeUnit.SECONDS), "pool finished in time");
        for (String id : new String[] { a, b, c, d }) {
            check(service.get(id).status == Status.COMPLETED, id + " is COMPLETED");
        }
        check(service.get(c).result.equals("text of c.pdf"), "c has its result");
    }

    static void check(boolean condition, String what) {
        if (!condition) {
            throw new AssertionError("FAILED: " + what);
        }
        System.out.println("ok: " + what);
    }

    public static void main(String[] args) throws InterruptedException {
        problem();
        solution();
    }
}
