// Facade Design Pattern Example
public class FacadeExample {
    static class CPU { void freeze() { System.out.println("CPU freeze"); } void jump(long pos) { System.out.println("CPU jump " + pos); } void execute() { System.out.println("CPU execute"); } }
    static class Memory { void load(long pos, byte[] data) { System.out.println("Memory load at " + pos + ", size=" + data.length); } }
    static class HardDrive { byte[] read(long lba, int size) { System.out.println("Disk read " + size + " bytes at " + lba); return new byte[size]; } }

    // Facade
    static class Computer {
        private final CPU cpu = new CPU();
        private final Memory ram = new Memory();
        private final HardDrive hdd = new HardDrive();
        void start() {
            cpu.freeze();
            ram.load(0, hdd.read(0, 1024));
            cpu.jump(0);
            cpu.execute();
        }
    }

    public static void main(String[] args) {
        Computer pc = new Computer();
        pc.start();
    }
}
