import java.util.*;

public class DeadLetterQueueExample {
    static class Broker {
        private final Queue<String> main = new ArrayDeque<>();
        private final Queue<String> dlq = new ArrayDeque<>();
        void publish(String m){ main.add(m); }
        void consume(){
            while (!main.isEmpty()){
                String m = main.poll();
                int attempts = 0;
                boolean success = false;
                while (attempts < 3 && !success){
                    attempts++;
                    try {
                        handle(m);
                        success = true;
                    } catch (RuntimeException e){ System.out.println("Fail on attempt " + attempts + " for " + m); }
                }
                if (!success){ System.out.println("Send to DLQ: " + m); dlq.add(m); }
            }
        }
        void handle(String m){ if (m.contains("bad")) throw new RuntimeException("processing error"); System.out.println("Processed: " + m); }
        Queue<String> getDlq(){ return dlq; }
    }
    public static void main(String[] args){
        Broker b = new Broker();
        b.publish("msg-1");
        b.publish("bad-msg-2");
        b.publish("msg-3");
        b.consume();
        System.out.println("DLQ -> " + b.getDlq());
    }
}
