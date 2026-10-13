import java.lang.management.ManagementFactory;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.*;

public class SchedulerDemo {
    static long T0;

    static void log(String s) {
        System.out.printf("%5d ms  [%-9s] %s%n", System.currentTimeMillis() - T0, Thread.currentThread().getName(), s);
    }

    // ---------- Part 1: the real ScheduledExecutorService, observed from outside
    // ----------
    static void real() throws Exception {
        Thread[] timer = new Thread[1];
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            timer[0] = new Thread(r, "timer");
            return timer[0];
        });

        T0 = System.currentTimeMillis();
        // scheduled OUT of order on purpose: 3 s, 1 s, 2 s
        scheduler.schedule(() -> log("run: cancel job C"), 3000, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> log("run: cancel job A"), 1000, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> log("run: cancel job B"), 2000, TimeUnit.MILLISECONDS);

        for (int i = 0; i < 4; i++) { // main peeks at the timer thread between runs
            Thread.sleep(i == 0 ? 500 : 1000);
            log("timer state = " + timer[0].getState());
        }
        long cpuMs = ManagementFactory.getThreadMXBean().getThreadCpuTime(timer[0].threadId()) / 1_000_000;
        log("timer thread total CPU over 3.5 s = " + cpuMs + " ms");
        scheduler.shutdown();
    }

    // ---------- Part 2: the same idea written by hand ----------
    record Timed(long dueAt, Runnable task) {
    }

    static void handMade() throws Exception {
        PriorityQueue<Timed> queue = new PriorityQueue<>(Comparator.comparingLong(Timed::dueAt)); // sorted by due time
        T0 = System.currentTimeMillis();
        queue.add(new Timed(T0 + 3000, () -> log("run: cancel job C")));
        queue.add(new Timed(T0 + 1000, () -> log("run: cancel job A")));
        queue.add(new Timed(T0 + 2000, () -> log("run: cancel job B")));

        Thread timer = new Thread(() -> {
            try {
                while (!queue.isEmpty()) {
                    long wait = queue.peek().dueAt() - System.currentTimeMillis(); // earliest task only
                    if (wait > 0) {
                        log("earliest task is due in " + wait + " ms -> park exactly that long");
                        Thread.sleep(wait);
                    }
                    queue.poll().task().run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "my-timer");
        timer.start();
        timer.join();
        // not handled here: a task added while the timer is parked (the real one wakes
        // the timer; that is wait/notify)
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Part 1: real ScheduledExecutorService ===");
        real();
        System.out.println("\n=== Part 2: hand-made, 12 lines ===");
        handMade();
    }
}
