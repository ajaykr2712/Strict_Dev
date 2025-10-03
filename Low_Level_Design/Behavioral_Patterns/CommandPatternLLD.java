// Command Pattern - Low Level Design Implementation

/**
 * Command Pattern Implementation
 * 
 * Real-world Use Case: Smart Home Automation System
 * - Control various devices (lights, fan, TV, music system)
 * - Support for macro commands (multiple operations)
 * - Undo/Redo functionality
 * - Remote control and voice commands
 * - Command queuing and scheduling
 * 
 * Key Components:
 * 1. Command Interface - Common interface for all commands
 * 2. Concrete Commands - Specific device operations  
 * 3. Receiver - Device that performs the actual work
 * 4. Invoker - Remote control that executes commands
 * 5. Client - Sets up command objects
 */

import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Command interface
interface Command {
    void execute();
    void undo();
    String getDescription();
    boolean isUndoable();
}

// Receiver interfaces - devices that execute commands
interface Device {
    String getName();
    String getStatus();
    void powerOn();
    void powerOff();
    boolean isPoweredOn();
}

interface DimmableDevice extends Device {
    void setBrightness(int level); // 0-100
    int getBrightness();
}

interface VolumeControlDevice extends Device {
    void setVolume(int level); // 0-100  
    int getVolume();
    void mute();
    void unmute();
    boolean isMuted();
}

// Concrete Receivers - Smart Home Devices
class SmartLight implements DimmableDevice {
    private String name;
    private boolean isOn;
    private int brightness;
    
    public SmartLight(String name) {
        this.name = name;
        this.isOn = false;
        this.brightness = 0;
    }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public String getStatus() {
        if (!isOn) return "OFF";
        return "ON (Brightness: " + brightness + "%)";
    }
    
    @Override
    public void powerOn() {
        this.isOn = true;
        if (brightness == 0) brightness = 50; // Default brightness
        logAction("turned ON");
    }
    
    @Override
    public void powerOff() {
        this.isOn = false;
        logAction("turned OFF");
    }
    
    @Override
    public boolean isPoweredOn() { return isOn; }
    
    @Override
    public void setBrightness(int level) {
        if (level < 0 || level > 100) {
            throw new IllegalArgumentException("Brightness must be 0-100");
        }
        this.brightness = level;
        this.isOn = level > 0;
        logAction("brightness set to " + level + "%");
    }
    
    @Override
    public int getBrightness() { return brightness; }
    
    private void logAction(String action) {
        System.out.println("💡 " + name + " " + action);
    }
}

class SmartSpeaker implements VolumeControlDevice {
    private String name;
    private boolean isOn;
    private int volume;
    private boolean isMuted;
    private int volumeBeforeMute;
    
    public SmartSpeaker(String name) {
        this.name = name;
        this.isOn = false;
        this.volume = 0;
        this.isMuted = false;
    }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public String getStatus() {
        if (!isOn) return "OFF";
        String status = "ON (Volume: " + volume + "%)";
        if (isMuted) status += " [MUTED]";
        return status;
    }
    
    @Override
    public void powerOn() {
        this.isOn = true;
        if (volume == 0) volume = 30; // Default volume
        logAction("turned ON");
    }
    
    @Override
    public void powerOff() {
        this.isOn = false;
        logAction("turned OFF");
    }
    
    @Override
    public boolean isPoweredOn() { return isOn; }
    
    @Override
    public void setVolume(int level) {
        if (level < 0 || level > 100) {
            throw new IllegalArgumentException("Volume must be 0-100");
        }
        this.volume = level;
        this.isMuted = false; // Unmute when volume is set
        if (level > 0) this.isOn = true;
        logAction("volume set to " + level + "%");
    }
    
    @Override
    public int getVolume() { return volume; }
    
    @Override
    public void mute() {
        if (!isMuted) {
            volumeBeforeMute = volume;
            isMuted = true;
            logAction("MUTED");
        }
    }
    
    @Override
    public void unmute() {
        if (isMuted) {
            isMuted = false;
            volume = volumeBeforeMute;
            logAction("UNMUTED (Volume: " + volume + "%)");
        }
    }
    
    @Override
    public boolean isMuted() { return isMuted; }
    
    private void logAction(String action) {
        System.out.println("🔊 " + name + " " + action);
    }
}

class SmartTV implements VolumeControlDevice {
    private String name;
    private boolean isOn;
    private int volume;
    private boolean isMuted;
    private int volumeBeforeMute;
    private int channel;
    
    public SmartTV(String name) {
        this.name = name;
        this.isOn = false;
        this.volume = 0;
        this.isMuted = false;
        this.channel = 1;
    }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public String getStatus() {
        if (!isOn) return "OFF";
        String status = "ON (Channel: " + channel + ", Volume: " + volume + "%)";
        if (isMuted) status += " [MUTED]";
        return status;
    }
    
    // TV specific methods
    public void setChannel(int ch) {
        if (ch < 1 || ch > 999) {
            throw new IllegalArgumentException("Channel must be 1-999");
        }
        this.channel = ch;
        if (!isOn) this.isOn = true;
        logAction("changed to channel " + ch);
    }
    
    public int getChannel() { return channel; }
    
    // Device interface implementation
    @Override
    public void powerOn() {
        this.isOn = true;
        if (volume == 0) volume = 25;
        logAction("turned ON");
    }
    
    @Override
    public void powerOff() {
        this.isOn = false;
        logAction("turned OFF");
    }
    
    @Override
    public boolean isPoweredOn() { return isOn; }
    
    @Override
    public void setVolume(int level) {
        if (level < 0 || level > 100) {
            throw new IllegalArgumentException("Volume must be 0-100");
        }
        this.volume = level;
        this.isMuted = false;
        if (level > 0) this.isOn = true;
        logAction("volume set to " + level + "%");
    }
    
    @Override
    public int getVolume() { return volume; }
    
    @Override
    public void mute() {
        if (!isMuted) {
            volumeBeforeMute = volume;
            isMuted = true;
            logAction("MUTED");
        }
    }
    
    @Override
    public void unmute() {
        if (isMuted) {
            isMuted = false;
            volume = volumeBeforeMute;
            logAction("UNMUTED (Volume: " + volume + "%)");
        }
    }
    
    @Override
    public boolean isMuted() { return isMuted; }
    
    private void logAction(String action) {
        System.out.println("📺 " + name + " " + action);
    }
}

// Concrete Commands
class PowerOnCommand implements Command {
    private Device device;
    
    public PowerOnCommand(Device device) {
        this.device = device;
    }
    
    @Override
    public void execute() {
        device.powerOn();
    }
    
    @Override
    public void undo() {
        device.powerOff();
    }
    
    @Override
    public String getDescription() {
        return "Turn ON " + device.getName();
    }
    
    @Override
    public boolean isUndoable() { return true; }
}

class PowerOffCommand implements Command {
    private Device device;
    
    public PowerOffCommand(Device device) {
        this.device = device;
    }
    
    @Override
    public void execute() {
        device.powerOff();
    }
    
    @Override
    public void undo() {
        device.powerOn();
    }
    
    @Override
    public String getDescription() {
        return "Turn OFF " + device.getName();
    }
    
    @Override
    public boolean isUndoable() { return true; }
}

class SetBrightnessCommand implements Command {
    private DimmableDevice device;
    private int newBrightness;
    private int previousBrightness;
    
    public SetBrightnessCommand(DimmableDevice device, int brightness) {
        this.device = device;
        this.newBrightness = brightness;
    }
    
    @Override
    public void execute() {
        this.previousBrightness = device.getBrightness();
        device.setBrightness(newBrightness);
    }
    
    @Override
    public void undo() {
        device.setBrightness(previousBrightness);
    }
    
    @Override
    public String getDescription() {
        return "Set " + device.getName() + " brightness to " + newBrightness + "%";
    }
    
    @Override
    public boolean isUndoable() { return true; }
}

class SetVolumeCommand implements Command {
    private VolumeControlDevice device;
    private int newVolume;
    private int previousVolume;
    
    public SetVolumeCommand(VolumeControlDevice device, int volume) {
        this.device = device;
        this.newVolume = volume;
    }
    
    @Override
    public void execute() {
        this.previousVolume = device.getVolume();
        device.setVolume(newVolume);
    }
    
    @Override
    public void undo() {
        device.setVolume(previousVolume);
    }
    
    @Override
    public String getDescription() {
        return "Set " + device.getName() + " volume to " + newVolume + "%";
    }
    
    @Override
    public boolean isUndoable() { return true; }
}

class ChangeChannelCommand implements Command {
    private SmartTV tv;
    private int newChannel;
    private int previousChannel;
    
    public ChangeChannelCommand(SmartTV tv, int channel) {
        this.tv = tv;
        this.newChannel = channel;
    }
    
    @Override
    public void execute() {
        this.previousChannel = tv.getChannel();
        tv.setChannel(newChannel);
    }
    
    @Override
    public void undo() {
        tv.setChannel(previousChannel);
    }
    
    @Override
    public String getDescription() {
        return "Change " + tv.getName() + " to channel " + newChannel;
    }
    
    @Override
    public boolean isUndoable() { return true; }
}

// Null Object Pattern for empty command slots
class NoCommand implements Command {
    @Override
    public void execute() {
        // Do nothing
    }
    
    @Override
    public void undo() {
        // Do nothing
    }
    
    @Override
    public String getDescription() {
        return "No Command Assigned";
    }
    
    @Override
    public boolean isUndoable() { return false; }
}

// Macro Command - composite command
class MacroCommand implements Command {
    private List<Command> commands;
    private String description;
    
    public MacroCommand(String description, Command... commands) {
        this.description = description;
        this.commands = Arrays.asList(commands);
    }
    
    @Override
    public void execute() {
        System.out.println("🎬 Executing macro: " + description);
        commands.forEach(Command::execute);
    }
    
    @Override
    public void undo() {
        System.out.println("⏪ Undoing macro: " + description);
        // Undo in reverse order
        for (int i = commands.size() - 1; i >= 0; i--) {
            Command cmd = commands.get(i);
            if (cmd.isUndoable()) {
                cmd.undo();
            }
        }
    }
    
    @Override
    public String getDescription() {
        return "Macro: " + description;
    }
    
    @Override
    public boolean isUndoable() {
        return commands.stream().anyMatch(Command::isUndoable);
    }
}

// Invoker - Smart Remote Control
class SmartRemoteControl {
    private Command[] onCommands;
    private Command[] offCommands;
    private Stack<Command> undoStack;
    private Queue<Command> commandQueue;
    private ExecutorService executor;
    private final int numSlots;
    
    public SmartRemoteControl(int numSlots) {
        this.numSlots = numSlots;
        this.onCommands = new Command[numSlots];
        this.offCommands = new Command[numSlots];
        this.undoStack = new Stack<>();
        this.commandQueue = new LinkedList<>();
        this.executor = Executors.newSingleThreadExecutor();
        
        // Initialize with null objects
        Command noCommand = new NoCommand();
        for (int i = 0; i < numSlots; i++) {
            onCommands[i] = noCommand;
            offCommands[i] = noCommand;
        }
    }
    
    public void setCommand(int slot, Command onCommand, Command offCommand) {
        if (slot < 0 || slot >= numSlots) {
            throw new IllegalArgumentException("Invalid slot number");
        }
        onCommands[slot] = onCommand;
        offCommands[slot] = offCommand;
    }
    
    public void onButtonPressed(int slot) {
        if (slot < 0 || slot >= numSlots) {
            throw new IllegalArgumentException("Invalid slot number");
        }
        Command command = onCommands[slot];
        executeCommand(command);
    }
    
    public void offButtonPressed(int slot) {
        if (slot < 0 || slot >= numSlots) {
            throw new IllegalArgumentException("Invalid slot number");
        }
        Command command = offCommands[slot];
        executeCommand(command);
    }
    
    private void executeCommand(Command command) {
        command.execute();
        if (command.isUndoable()) {
            undoStack.push(command);
        }
        logCommand(command);
    }
    
    public void undoButtonPressed() {
        if (!undoStack.isEmpty()) {
            Command lastCommand = undoStack.pop();
            System.out.println("⏪ Undoing: " + lastCommand.getDescription());
            lastCommand.undo();
        } else {
            System.out.println("❌ No command to undo");
        }
    }
    
    // Asynchronous command execution
    public void queueCommand(Command command) {
        commandQueue.offer(command);
        System.out.println("📥 Queued: " + command.getDescription());
    }
    
    public void executeQueuedCommands() {
        if (commandQueue.isEmpty()) {
            System.out.println("📭 No queued commands");
            return;
        }
        
        System.out.println("🚀 Executing queued commands...");
        executor.submit(() -> {
            while (!commandQueue.isEmpty()) {
                Command command = commandQueue.poll();
                executeCommand(command);
                try {
                    Thread.sleep(500); // Simulate processing time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    public void printRemoteStatus() {
        System.out.println("\\n🎛️  REMOTE CONTROL STATUS");
        System.out.println("========================");
        for (int i = 0; i < numSlots; i++) {
            System.out.println("Slot " + i + " - ON: " + onCommands[i].getDescription());
            System.out.println("      - OFF: " + offCommands[i].getDescription());
        }
        System.out.println("Undo Stack Size: " + undoStack.size());
        System.out.println("Queued Commands: " + commandQueue.size());
    }
    
    private void logCommand(Command command) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("⏰ [" + timestamp + "] Executed: " + command.getDescription());
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}

// Smart Home System - Client
class SmartHomeSystem {
    private SmartRemoteControl remote;
    private Map<String, Device> devices;
    
    public SmartHomeSystem() {
        this.remote = new SmartRemoteControl(6); // 6 slots
        this.devices = new HashMap<>();
        setupDevices();
        setupRemoteCommands();
    }
    
    private void setupDevices() {
        // Create smart devices
        devices.put("living_room_light", new SmartLight("Living Room Light"));
        devices.put("bedroom_light", new SmartLight("Bedroom Light"));
        devices.put("kitchen_light", new SmartLight("Kitchen Light"));
        devices.put("sound_system", new SmartSpeaker("Sound System"));
        devices.put("main_tv", new SmartTV("Main TV"));
    }
    
    private void setupRemoteCommands() {
        SmartLight livingRoomLight = (SmartLight) devices.get("living_room_light");
        SmartLight bedroomLight = (SmartLight) devices.get("bedroom_light");
        SmartSpeaker soundSystem = (SmartSpeaker) devices.get("sound_system");
        SmartTV mainTV = (SmartTV) devices.get("main_tv");
        
        // Slot 0 - Living Room Light
        remote.setCommand(0, 
            new PowerOnCommand(livingRoomLight),
            new PowerOffCommand(livingRoomLight));
        
        // Slot 1 - Sound System  
        remote.setCommand(1,
            new PowerOnCommand(soundSystem),
            new PowerOffCommand(soundSystem));
        
        // Slot 2 - Main TV
        remote.setCommand(2,
            new PowerOnCommand(mainTV),
            new PowerOffCommand(mainTV));
        
        // Slot 3 - Bedroom Light with brightness
        remote.setCommand(3,
            new SetBrightnessCommand(bedroomLight, 75),
            new PowerOffCommand(bedroomLight));
        
        // Slot 4 - Movie Mode Macro
        Command movieMode = new MacroCommand("Movie Mode",
            new SetBrightnessCommand(livingRoomLight, 20), // Dim lights
            new PowerOnCommand(mainTV),
            new ChangeChannelCommand(mainTV, 150), // Movie channel
            new SetVolumeCommand(mainTV, 60),
            new PowerOnCommand(soundSystem),
            new SetVolumeCommand(soundSystem, 70)
        );
        
        remote.setCommand(4, movieMode, new NoCommand());
        
        // Slot 5 - Sleep Mode Macro
        Command sleepMode = new MacroCommand("Sleep Mode",
            new PowerOffCommand(mainTV),
            new PowerOffCommand(soundSystem),
            new PowerOffCommand(livingRoomLight),
            new SetBrightnessCommand(bedroomLight, 10) // Night light
        );
        
        remote.setCommand(5, sleepMode, new NoCommand());
    }
    
    public SmartRemoteControl getRemote() {
        return remote;
    }
    
    public void printDeviceStatus() {
        System.out.println("\\n🏠 SMART HOME DEVICE STATUS");
        System.out.println("============================");
        devices.forEach((name, device) -> {
            System.out.println(device.getName() + ": " + device.getStatus());
        });
    }
    
    public Device getDevice(String deviceId) {
        return devices.get(deviceId);
    }
    
    public void shutdown() {
        remote.shutdown();
    }
}

// Main demonstration class
public class CommandPatternLLD {
    public static void main(String[] args) {
        System.out.println("🏠 SMART HOME AUTOMATION SYSTEM - Command Pattern Demo");
        System.out.println("======================================================\\n");
        
        // Create smart home system
        SmartHomeSystem smartHome = new SmartHomeSystem();
        SmartRemoteControl remote = smartHome.getRemote();
        
        // Show initial status
        smartHome.printDeviceStatus();
        remote.printRemoteStatus();
        
        System.out.println("\\n🎮 TESTING INDIVIDUAL COMMANDS");
        System.out.println("==============================");
        
        // Test individual device commands
        System.out.println("\\n--- Turning on devices ---");
        remote.onButtonPressed(0); // Living room light
        remote.onButtonPressed(1); // Sound system
        remote.onButtonPressed(2); // TV
        
        System.out.println("\\n--- Current device status ---");
        smartHome.printDeviceStatus();
        
        System.out.println("\\n--- Testing undo functionality ---");
        remote.undoButtonPressed(); // Undo TV
        remote.undoButtonPressed(); // Undo Sound system
        
        System.out.println("\\n--- After undo operations ---");
        smartHome.printDeviceStatus();
        
        System.out.println("\\n🎬 TESTING MACRO COMMANDS");
        System.out.println("=========================");
        
        // Test Movie Mode macro
        System.out.println("\\n--- Activating Movie Mode ---");
        remote.onButtonPressed(4); // Movie mode macro
        
        System.out.println("\\n--- Movie mode activated ---");
        smartHome.printDeviceStatus();
        
        // Test undo macro
        System.out.println("\\n--- Undoing Movie Mode ---");
        remote.undoButtonPressed();
        
        System.out.println("\\n--- After undoing Movie Mode ---");
        smartHome.printDeviceStatus();
        
        System.out.println("\\n--- Activating Sleep Mode ---");
        remote.onButtonPressed(5); // Sleep mode macro
        
        System.out.println("\\n--- Sleep mode activated ---");
        smartHome.printDeviceStatus();
        
        System.out.println("\\n🔄 TESTING COMMAND QUEUING");
        System.out.println("==========================");
        
        // Test command queuing
        SmartLight kitchenLight = (SmartLight) smartHome.getDevice("kitchen_light");
        SmartTV mainTV = (SmartTV) smartHome.getDevice("main_tv");
        
        remote.queueCommand(new PowerOnCommand(kitchenLight));
        remote.queueCommand(new SetBrightnessCommand(kitchenLight, 80));
        remote.queueCommand(new PowerOnCommand(mainTV));
        remote.queueCommand(new ChangeChannelCommand(mainTV, 25));
        
        remote.executeQueuedCommands();
        
        // Wait for queued commands to complete
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\\n--- Final device status ---");
        smartHome.printDeviceStatus();
        
        // Cleanup
        smartHome.shutdown();
        
        System.out.println("\\n✅ Command Pattern Demo Complete!");
        
        System.out.println("\\n📚 KEY CONCEPTS DEMONSTRATED:");
        System.out.println("• Command Interface - Common interface for all operations");
        System.out.println("• Concrete Commands - Specific device operations");
        System.out.println("• Receivers - Smart devices that perform actual work");
        System.out.println("• Invoker - Remote control that executes commands");
        System.out.println("• Macro Commands - Composite operations");
        System.out.println("• Undo/Redo - Reversible operations");
        System.out.println("• Command Queuing - Asynchronous execution");
        System.out.println("• Null Object Pattern - Default empty commands");
    }
}
