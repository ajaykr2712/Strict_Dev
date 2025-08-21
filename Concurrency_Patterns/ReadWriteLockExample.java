// Concurrency: ReadWriteLock Example
import java.util.concurrent.locks.*;

public class ReadWriteLockExample {
    static class SafeCounter {
        private final ReadWriteLock rw = new ReentrantReadWriteLock();
        private int value = 0;
        public void increment() { rw.writeLock().lock(); try { value++; } finally { rw.writeLock().unlock(); } }
        public int get() { rw.readLock().lock(); try { return value; } finally { rw.readLock().unlock(); } }
    }

    public static void main(String[] args) throws InterruptedException {
        SafeCounter c = new SafeCounter();
        Thread writer = new Thread(() -> { for (int i=0;i<1000;i++) c.increment(); });
        Thread reader1 = new Thread(() -> { for (int i=0;i<5;i++) System.out.println("R1 sees " + c.get()); });
        Thread reader2 = new Thread(() -> { for (int i=0;i<5;i++) System.out.println("R2 sees " + c.get()); });
        writer.start(); reader1.start(); reader2.start();
        writer.join(); reader1.join(); reader2.join();
        System.out.println("Final value: " + c.get());
    }
}
