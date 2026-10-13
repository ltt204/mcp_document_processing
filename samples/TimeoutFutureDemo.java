public class TimeoutFutureDemo {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Callable<String> task = () -> {
            Thread.sleep(5000); // Simulate a long-running task
            return "Task completed";
        };

        Future<String> future = executorService.submit(task);

        try {
            // Wait for the result with a timeout of 2 seconds
            String result = future.get(2, TimeUnit.SECONDS);
            System.out.println(result);
        } catch (TimeoutException e) {
            System.out.println("Task timed out");
            future.cancel(true); // Cancel the task if it times out
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }
}
