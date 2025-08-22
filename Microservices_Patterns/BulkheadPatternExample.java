import java.util.concurrent.*;

public class BulkheadPatternExample {
    static class Bulkhead {
        private final ExecutorService pool;
        Bulkhead(int size){ this.pool = new ThreadPoolExecutor(size, size, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(size), new ThreadPoolExecutor.AbortPolicy()); }
        Future<?> submit(Runnable task){ return pool.submit(task); }
        void shutdown(){ pool.shutdown(); }
    }

    public static void main(String[] args) throws Exception {
        Bulkhead payments = new Bulkhead(2);
        Bulkhead search = new Bulkhead(8);
        Runnable slow = () -> { try { Thread.sleep(500); } catch (InterruptedException e){ Thread.currentThread().interrupt(); } };
        for (int i=0;i<10;i++){ int id=i; payments.submit(() -> { System.out.println("payments task " + id); slow.run(); }); }
        for (int i=0;i<10;i++){ int id=i; search.submit(() -> { System.out.println("search task " + id); slow.run(); }); }
        payments.shutdown(); search.shutdown();
        System.out.println("Bulkheads isolate failures and load");
    }
}
