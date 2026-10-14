import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class CancelDemo {
    static final long T0 = System.currentTimeMillis();
    static void log(String s) { System.out.printf("%5d ms  %s%n", System.currentTimeMillis() - T0, s); }

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);   // ONE worker
        AtomicReference<Thread> worker = new AtomicReference<>();

        Future<?> a = pool.submit(() -> {                          // A: pure CPU for 3 s, like a parser
            worker.set(Thread.currentThread());
            log("A started");
            long x = 0;
            while (System.currentTimeMillis() - T0 < 3000) x++;
            log("A reached its last line. flag = " + Thread.currentThread().isInterrupted());
        });
        Future<?> c = pool.submit(() -> log("C started. flag = " + Thread.currentThread().isInterrupted()));

        Thread.sleep(500);
        log("c.cancel(true) returned " + c.cancel(true) + "   (C is in the queue)");
        log("a.cancel(true) returned " + a.cancel(true) + "   (A is mid-loop)");

        Future<?> d = pool.submit(() -> log("D started"));          // a new job arrives

        Thread.sleep(1000);
        log("1 s after cancel: a.isDone() = " + a.isDone() + ", worker state = " + worker.get().getState());

        d.get();
        pool.shutdown();
    }
}
