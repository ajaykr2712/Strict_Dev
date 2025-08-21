// Concurrency: CompletableFuture Example
import java.util.concurrent.*;

public class CompletableFutureExample {
    public static void log(String s) { System.out.println("[" + Thread.currentThread().getName() + "] " + s); }
    public static void main(String[] args) throws Exception {
        CompletableFuture<Integer> a = CompletableFuture.supplyAsync(() -> { log("Compute A"); sleep(300); return 10; });
        CompletableFuture<Integer> b = CompletableFuture.supplyAsync(() -> { log("Compute B"); sleep(200); return 5; });
        CompletableFuture<Integer> sum = a.thenCombine(b, Integer::sum);
        log("Sum: " + sum.get());
    }
    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
}
