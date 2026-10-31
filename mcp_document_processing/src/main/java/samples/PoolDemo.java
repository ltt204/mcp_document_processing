package samples;

// import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PoolDemo {
    public static void main(String[] args) {
        // try (ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 4, 0L,
        // TimeUnit.MILLISECONDS,
        // new ArrayBlockingQueue<>(2))) {
        try (ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 4, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>())) {
            for (int i = 0; i < 10; i++) {
                final int jobId = i;
                try {
                    System.out.println("submit " + jobId);
                    pool.execute(() -> {
                        System.out.println("Running job " + jobId + " in thread " + Thread.currentThread().getName());

                        try {
                            Thread.sleep(2000); // Simulate work
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    System.out.println("Job " + jobId + " rejected: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
