import java.util.concurrent.Phaser;

public class PhaserExample {
    public static void main(String[] args) throws InterruptedException {
        Phaser phaser = new Phaser(1); // main thread registers
        for (int i = 0; i < 3; i++){
            phaser.register();
            final int id = i;
            new Thread(() -> {
                System.out.println("T" + id + " phase " + phaser.getPhase());
                phaser.arriveAndAwaitAdvance();
                System.out.println("T" + id + " phase " + phaser.getPhase());
                phaser.arriveAndDeregister();
            }).start();
        }
        Thread.sleep(200);
        System.out.println("Main advancing phase");
        phaser.arriveAndAwaitAdvance();
        phaser.arriveAndDeregister();
    }
}
