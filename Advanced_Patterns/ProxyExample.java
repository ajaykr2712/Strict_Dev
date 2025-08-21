// Proxy Design Pattern Example
public class ProxyExample {
    interface Image { void display(); }

    static class RealImage implements Image {
        private final String fileName;
        RealImage(String fileName) { this.fileName = fileName; loadFromDisk(); }
        private void loadFromDisk() { System.out.println("Loading " + fileName); }
        @Override public void display() { System.out.println("Displaying " + fileName); }
    }

    static class ProxyImage implements Image {
        private final String fileName; private RealImage real;
        ProxyImage(String fileName) { this.fileName = fileName; }
        @Override public void display() { if (real == null) real = new RealImage(fileName); real.display(); }
    }

    public static void main(String[] args) {
        Image img = new ProxyImage("photo.png");
        img.display(); // loads + displays
        img.display(); // cached display
    }
}
