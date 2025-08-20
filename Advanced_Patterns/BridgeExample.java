// Bridge Design Pattern Example
public class BridgeExample {
    // Implementor
    interface Device {
        void enable();
        void disable();
        void setVolume(int percent);
        boolean isEnabled();
        int getVolume();
    }

    // Concrete Implementors
    static class TV implements Device {
        private boolean on;
        private int volume = 30;
        public void enable() { on = true; System.out.println("TV: ON"); }
        public void disable() { on = false; System.out.println("TV: OFF"); }
        public void setVolume(int percent) { volume = Math.max(0, Math.min(100, percent)); System.out.println("TV Volume: " + volume); }
        public boolean isEnabled() { return on; }
        public int getVolume() { return volume; }
    }

    static class Radio implements Device {
        private boolean on;
        private int volume = 20;
        public void enable() { on = true; System.out.println("Radio: ON"); }
        public void disable() { on = false; System.out.println("Radio: OFF"); }
        public void setVolume(int percent) { volume = Math.max(0, Math.min(100, percent)); System.out.println("Radio Volume: " + volume); }
        public boolean isEnabled() { return on; }
        public int getVolume() { return volume; }
    }

    // Abstraction
    static class RemoteControl {
        protected final Device device;
        RemoteControl(Device device) { this.device = device; }
        void togglePower() { if (device.isEnabled()) device.disable(); else device.enable(); }
        void volumeDown() { device.setVolume(device.getVolume() - 10); }
        void volumeUp() { device.setVolume(device.getVolume() + 10); }
    }

    // Refined Abstraction
    static class AdvancedRemoteControl extends RemoteControl {
        AdvancedRemoteControl(Device device) { super(device); }
        void mute() { device.setVolume(0); System.out.println("Muted"); }
    }

    public static void main(String[] args) {
        Device tv = new TV();
        RemoteControl basic = new RemoteControl(tv);
        basic.togglePower();
        basic.volumeUp();

        Device radio = new Radio();
        AdvancedRemoteControl advanced = new AdvancedRemoteControl(radio);
        advanced.togglePower();
        advanced.volumeUp();
        advanced.mute();
        advanced.togglePower();
    }
}
