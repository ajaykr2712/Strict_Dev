import java.util.concurrent.*;

public class CyclicBarrierExample {
    public static void main(String[] args) throws Exception {
        int parties = 3;
        CyclicBarrier barrier = new CyclicBarrier(parties, () -> System.out.println("All parties reached barrier, proceeding"));
        ExecutorService pool = Executors.newFixedThreadPool(parties);
        for (int i = 0; i < parties; i++){
            final int id = i;
            pool.submit(() -> {
                try {
                    System.out.println("Worker " + id + " doing phase 1");
                    Thread.sleep(200 + id * 100);
                    barrier.await();
                    System.out.println("Worker " + id + " doing phase 2");
                } catch (Exception e){ Thread.currentThread().interrupt(); }
            });
        }
        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.SECONDS);
    }
}
