import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// Run with: java JobEngineDemo.java
public class JobEngineDemo {
    // The status of a job.
    enum Status {
        PENDING, IN_PROGRESS, COMPLETED, ERROR
    }

    // One job: what to do, and what happened to it.
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

    // Stands in for PDFParsingService: slow work that can fail.
    static String parse(String path) throws InterruptedException {
        Thread.sleep(3000);
        if (path.contains("bad")) {
            throw new IllegalStateException("not a PDF");
        }
        return "text of " + path;
    }

    static class JobService {
        private final Map<String, Job> jobs = new ConcurrentHashMap<>(); // the job store
        private final ExecutorService pool; // the threads and the queue
        private int nextId = 1;

        JobService(ExecutorService pool) {
            this.pool = pool;
        }

        // Runs on the CALLER's thread and returns at once.
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

        // Runs on a WORKER thread, whenever one becomes free.
        private void run(Job job) {
            job.status = Status.IN_PROGRESS;
            log("start  " + job.id);
            try {
                job.result = parse(job.path);
                job.status = Status.COMPLETED;
            } catch (Exception e) {
                job.result = e.getMessage();
                job.status = Status.ERROR;
            }
            log("finish " + job.id);
        }

        Job get(String id) {
            return jobs.get(id);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // The root: build the pool, give it to the service. 2 threads, 2 waiting slots.
        ExecutorService pool = new ThreadPoolExecutor(
                2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2));
        JobService service = new JobService(pool);

        List<String> ids = new ArrayList<>();
        for (String path : List.of("a.pdf", "b.pdf", "bad.pdf", "d.pdf", "e.pdf")) {
            try {
                String id = service.submit(path);
                ids.add(id);
                log("submit " + path + " -> " + id);
            } catch (IllegalStateException e) {
                log("submit " + path + " -> REFUSED: " + e.getMessage());
            }
        }

        for (int second = 0; second <= 7; second += 3) {
            StringBuilder line = new StringBuilder("t=" + second + "s ");
            for (String id : ids) {
                Job job = service.get(id);
                line.append(" ").append(id).append("=").append(job.status);
            }
            log(line.toString());
            Thread.sleep(3500);
        }

        for (String id : ids) {
            log("result " + id + ": " + service.get(id).result);
        }
        pool.shutdown();
    }

    static void log(String message) {
        System.out.printf("[%-15s] %s%n", Thread.currentThread().getName(), message);
    }
}
