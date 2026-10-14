class MyThread extends Thread {
    @Override
    public void run() {
        Thread currentThread = Thread.currentThread();
        System.out.println("Thread is going to park: " + currentThread.getName());
        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            System.out.println("Thread was interrupted while sleeping");
        }
        System.out.println("Thread is unparked: " + currentThread.getName());
    }

}

public class InterruptParkedThread {
    public static void main(String[] args) throws InterruptedException {
        MyThread thread = new MyThread();
        thread.start();

        // Wait for a moment to ensure the thread is parked
        Thread.sleep(1000);

        // Interrupt the parked thread
        System.out.println("Main thread is interrupting the parked thread");
        thread.interrupt();

        // Wait for the thread to finish
        thread.join();
        System.out.println("Main thread is exiting");
    }
}