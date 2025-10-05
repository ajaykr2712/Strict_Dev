import java.util.concurrent.*;

public class SemaphoreExample {
    public static void main(String[] args) throws InterruptedException {
        Semaphore permits = new Semaphore(3);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 10; i++){
            final int id = i;
            pool.submit(() -> {
                try {
                    permits.acquire();
                    System.out.println("Task " + id + " acquired permit");
                    Thread.sleep(300);
                } catch (InterruptedException e){ Thread.currentThread().interrupt(); }
                finally {
                    System.out.println("Task " + id + " releasing permit");
                    permits.release();
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("Done");
    }
}
