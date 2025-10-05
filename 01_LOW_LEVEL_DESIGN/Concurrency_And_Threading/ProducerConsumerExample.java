// Concurrency: Producer-Consumer with BlockingQueue
import java.util.concurrent.*;

public class ProducerConsumerExample {
    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                try { queue.put(i); System.out.println("Produced " + i); Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        });
        Thread consumer = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                try { Integer v = queue.take(); System.out.println("Consumed " + v); Thread.sleep(150); } catch (InterruptedException ignored) {}
            }
        });
        producer.start(); consumer.start();
        producer.join(); consumer.join();
        System.out.println("Done");
    }
}
