import java.util.concurrent.*;

public class ActorModelExample {
    interface Actor { void tell(Object msg); void stop(); }

    static class EchoActor implements Actor {
        private final BlockingQueue<Object> mailbox = new LinkedBlockingQueue<>();
        private volatile boolean running = true;
        EchoActor(){
            Thread t = new Thread(() -> {
                try {
                    while (running) {
                        Object m = mailbox.take();
                        if (m instanceof PoisonPill) break;
                        System.out.println("Echo: " + m);
                    }
                } catch (InterruptedException ie){ Thread.currentThread().interrupt(); }
            }, "echo-actor");
            t.setDaemon(true);
            t.start();
        }
        public void tell(Object msg){ mailbox.offer(msg); }
        public void stop(){ running = false; mailbox.offer(PoisonPill.INSTANCE); }
    }

    enum PoisonPill { INSTANCE }

    public static void main(String[] args) throws Exception {
        Actor echo = new EchoActor();
        echo.tell("Hello");
        echo.tell("Actor model in Java");
        Thread.sleep(200);
        echo.stop();
    }
}
