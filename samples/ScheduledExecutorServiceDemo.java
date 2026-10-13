public class ScheduledExecutorServiceDemo {
    public static void main(String[] args) {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        Runnable task = () -> System.out.println("Task executed at: " + System.currentTimeMillis());

        // Schedule the task to run after a delay of 2 seconds
        executorService.schedule(task, 2, TimeUnit.SECONDS);

        // Schedule the task to run repeatedly every 3 seconds, starting after an
        // initial delay of 1 second
        executorService.scheduleAtFixedRate(task, 1, 3, TimeUnit.SECONDS);

        // Schedule the task to run repeatedly with a fixed delay of 4 seconds between
        // the end of one execution and the start of the next
        executorService.scheduleWithFixedDelay(task, 1, 4, TimeUnit.SECONDS);

        executorService.schedule(() -> {
            System.out.println("Shutting down executor service");
            executorService.shutdown();
        }, 15, TimeUnit.SECONDS);
    }
}
