import java.time.*;
import java.util.function.Supplier;

public class RetryPatternExample {
    static class Retrier {
        private final int maxAttempts; private final long initialBackoffMs; private final double multiplier;
        Retrier(int maxAttempts, long initialBackoffMs, double multiplier){ this.maxAttempts = maxAttempts; this.initialBackoffMs = initialBackoffMs; this.multiplier = multiplier; }
        <T> T call(Supplier<T> op){
            long backoff = initialBackoffMs;
            RuntimeException last = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++){
                try { return op.get(); } catch (RuntimeException e){
                    last = e;
                    System.out.println("Attempt " + attempt + " failed: " + e.getMessage());
                    if (attempt == maxAttempts) break;
                    try { Thread.sleep(backoff); } catch (InterruptedException ie){ Thread.currentThread().interrupt(); throw new RuntimeException(ie); }
                    backoff = (long) Math.min(10_000, backoff * multiplier);
                }
            }
            throw last != null ? last : new RuntimeException("Unknown error");
        }
    }

    static class FlakyService {
        private int failuresLeft;
        FlakyService(int failures){ this.failuresLeft = failures; }
        String call(){
            if (failuresLeft-- > 0) throw new RuntimeException("temporary failure");
            return "success at " + Instant.now();
        }
    }

    public static void main(String[] args){
        FlakyService svc = new FlakyService(3);
        Retrier retrier = new Retrier(5, 100, 2.0);
        String result = retrier.call(svc::call);
        System.out.println(result);
    }
}
