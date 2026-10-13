import java.lang.management.ManagementFactory;
import java.util.concurrent.*;

public class IdleDemo {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(() -> {});            // make the pool start its 2 workers
        pool.submit(() -> {});
        Thread.sleep(200);

        var mx = ManagementFactory.getThreadMXBean();
        long[] before = new long[2];
        Thread[] workers = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getName().startsWith("pool-")).toArray(Thread[]::new);
        for (int i = 0; i < 2; i++) before[i] = mx.getThreadCpuTime(workers[i].threadId());

        Thread.sleep(3000);               // server idle for 3 seconds

        for (int i = 0; i < 2; i++) {
            long cpuMs = (mx.getThreadCpuTime(workers[i].threadId()) - before[i]) / 1_000_000;
            System.out.println(workers[i].getName() + "  state = " + workers[i].getState()
                    + "  CPU used in 3 s idle = " + cpuMs + " ms");
            System.out.println("    top of stack: " + workers[i].getStackTrace()[2]);
        }
        pool.shutdown();
    }
}
