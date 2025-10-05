import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Command Pattern Implementation
 * 
 * Real-world Use Case: Netflix Remote Control System
 * Implementing undo/redo functionality for user actions
 */

public class CommandExample {
    
    // Command interface
    interface Command {
        void execute();
        void undo();
        String getDescription();
        long getTimestamp();
    }
    
    // Receiver - Netflix Player
    static class NetflixPlayer {
        private String currentContent;
        private boolean isPlaying;
        private int volume;
        private int position; // in seconds
        private boolean isFullscreen;
        private String quality;
        
        public NetflixPlayer() {
            this.volume = 50;
            this.position = 0;
            this.isPlaying = false;
            this.isFullscreen = false;
            this.quality = "HD";
        }
        
        public void play(String content) {
            this.currentContent = content;
            this.isPlaying = true;
            System.out.println("Playing: " + content);
        }
        
        public void pause() {
            this.isPlaying = false;
            System.out.println("Paused: " + currentContent);
        }
        
        public void stop() {
            this.isPlaying = false;
            this.position = 0;
            System.out.println("Stopped: " + currentContent);
        }
        
        public void setVolume(int volume) {
            this.volume = Math.max(0, Math.min(100, volume));
            System.out.println("Volume set to: " + this.volume);
        }
        
        public void seek(int position) {
            this.position = position;
            System.out.println("Seeked to: " + position + " seconds");
        }
        
        public void setFullscreen(boolean fullscreen) {
            this.isFullscreen = fullscreen;
            System.out.println("Fullscreen: " + (fullscreen ? "ON" : "OFF"));
        }
        
        public void setQuality(String quality) {
            this.quality = quality;
            System.out.println("Quality set to: " + quality);
        }
        
        // Getters
        public String getCurrentContent() { return currentContent; }
        public boolean isPlaying() { return isPlaying; }
        public int getVolume() { return volume; }
        public int getPosition() { return position; }
        public boolean isFullscreen() { return isFullscreen; }
        public String getQuality() { return quality; }
        
        public void showStatus() {
            System.out.println(String.format(
                "Player Status: %s | Playing: %s | Volume: %d | Position: %d | Fullscreen: %s | Quality: %s",
                currentContent != null ? currentContent : "No content",
                isPlaying ? "Yes" : "No",
                volume, position, isFullscreen ? "Yes" : "No", quality
            ));
        }
    }
    
    // Concrete Commands
    static class PlayCommand implements Command {
        private final NetflixPlayer player;
        private final String content;
        private final String previousContent;
        private final boolean wasPlaying;
        private final long timestamp;
        
        public PlayCommand(NetflixPlayer player, String content) {
            this.player = player;
            this.content = content;
            this.previousContent = player.getCurrentContent();
            this.wasPlaying = player.isPlaying();
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public void execute() {
            player.play(content);
        }
        
        @Override
        public void undo() {
            if (previousContent != null) {
                if (wasPlaying) {
                    player.play(previousContent);
                } else {
                    player.pause();
                }
            } else {
                player.stop();
            }
        }
        
        @Override
        public String getDescription() {
            return "Play: " + content;
        }
        
        @Override
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    static class VolumeCommand implements Command {
        private final NetflixPlayer player;
        private final int newVolume;
        private final int previousVolume;
        private final long timestamp;
        
        public VolumeCommand(NetflixPlayer player, int newVolume) {
            this.player = player;
            this.newVolume = newVolume;
            this.previousVolume = player.getVolume();
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public void execute() {
            player.setVolume(newVolume);
        }
        
        @Override
        public void undo() {
            player.setVolume(previousVolume);
        }
        
        @Override
        public String getDescription() {
            return "Volume: " + previousVolume + " → " + newVolume;
        }
        
        @Override
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    static class SeekCommand implements Command {
        private final NetflixPlayer player;
        private final int newPosition;
        private final int previousPosition;
        private final long timestamp;
        
        public SeekCommand(NetflixPlayer player, int newPosition) {
            this.player = player;
            this.newPosition = newPosition;
            this.previousPosition = player.getPosition();
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public void execute() {
            player.seek(newPosition);
        }
        
        @Override
        public void undo() {
            player.seek(previousPosition);
        }
        
        @Override
        public String getDescription() {
            return "Seek: " + previousPosition + "s → " + newPosition + "s";
        }
        
        @Override
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    static class FullscreenCommand implements Command {
        private final NetflixPlayer player;
        private final boolean newFullscreen;
        private final boolean previousFullscreen;
        private final long timestamp;
        
        public FullscreenCommand(NetflixPlayer player, boolean newFullscreen) {
            this.player = player;
            this.newFullscreen = newFullscreen;
            this.previousFullscreen = player.isFullscreen();
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public void execute() {
            player.setFullscreen(newFullscreen);
        }
        
        @Override
        public void undo() {
            player.setFullscreen(previousFullscreen);
        }
        
        @Override
        public String getDescription() {
            return "Fullscreen: " + previousFullscreen + " → " + newFullscreen;
        }
        
        @Override
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    // Macro Command for grouped operations
    static class MacroCommand implements Command {
        private final List<Command> commands;
        private final String description;
        private final long timestamp;
        
        public MacroCommand(String description, List<Command> commands) {
            this.description = description;
            this.commands = new ArrayList<>(commands);
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public void execute() {
            for (Command command : commands) {
                command.execute();
            }
        }
        
        @Override
        public void undo() {
            // Undo in reverse order
            for (int i = commands.size() - 1; i >= 0; i--) {
                commands.get(i).undo();
            }
        }
        
        @Override
        public String getDescription() {
            return description + " (" + commands.size() + " operations)";
        }
        
        @Override
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    // Invoker - Remote Control
    static class NetflixRemoteControl {
        private final Stack<Command> undoStack;
        private final Stack<Command> redoStack;
        private final Map<String, Command> shortcuts;
        private final NetflixPlayer player;
        
        public NetflixRemoteControl(NetflixPlayer player) {
            this.player = player;
            this.undoStack = new Stack<>();
            this.redoStack = new Stack<>();
            this.shortcuts = new ConcurrentHashMap<>();
        }
        
        public void executeCommand(Command command) {
            command.execute();
            undoStack.push(command);
            redoStack.clear(); // Clear redo stack when new command is executed
            System.out.println("[REMOTE] Executed: " + command.getDescription());
        }
        
        public void undo() {
            if (!undoStack.isEmpty()) {
                Command command = undoStack.pop();
                command.undo();
                redoStack.push(command);
                System.out.println("[REMOTE] Undid: " + command.getDescription());
            } else {
                System.out.println("[REMOTE] Nothing to undo");
            }
        }
        
        public void redo() {
            if (!redoStack.isEmpty()) {
                Command command = redoStack.pop();
                command.execute();
                undoStack.push(command);
                System.out.println("[REMOTE] Redid: " + command.getDescription());
            } else {
                System.out.println("[REMOTE] Nothing to redo");
            }
        }
        
        public void saveShortcut(String name, Command command) {
            shortcuts.put(name, command);
            System.out.println("[REMOTE] Saved shortcut: " + name + " → " + command.getDescription());
        }
        
        public void executeShortcut(String name) {
            Command command = shortcuts.get(name);
            if (command != null) {
                executeCommand(command);
            } else {
                System.out.println("[REMOTE] Shortcut not found: " + name);
            }
        }
        
        public void showHistory() {
            System.out.println("\n[REMOTE] Command History:");
            if (undoStack.isEmpty()) {
                System.out.println("  No commands executed");
            } else {
                for (int i = undoStack.size() - 1; i >= 0; i--) {
                    Command cmd = undoStack.get(i);
                    System.out.println("  " + (undoStack.size() - i) + ". " + cmd.getDescription());
                }
            }
        }
        
        public void showShortcuts() {
            System.out.println("\n[REMOTE] Available Shortcuts:");
            if (shortcuts.isEmpty()) {
                System.out.println("  No shortcuts defined");
            } else {
                shortcuts.forEach((name, command) -> 
                    System.out.println("  " + name + " → " + command.getDescription())
                );
            }
        }
        
        // Quick action methods
        public void playContent(String content) {
            executeCommand(new PlayCommand(player, content));
        }
        
        public void adjustVolume(int volume) {
            executeCommand(new VolumeCommand(player, volume));
        }
        
        public void seekTo(int position) {
            executeCommand(new SeekCommand(player, position));
        }
        
        public void toggleFullscreen() {
            executeCommand(new FullscreenCommand(player, !player.isFullscreen()));
        }
        
        public void createTheaterMode() {
            List<Command> theaterCommands = Arrays.asList(
                new FullscreenCommand(player, true),
                new VolumeCommand(player, 80)
            );
            executeCommand(new MacroCommand("Theater Mode", theaterCommands));
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Command Pattern Demo: Netflix Remote Control ===\n");
        
        NetflixPlayer player = new NetflixPlayer();
        NetflixRemoteControl remote = new NetflixRemoteControl(player);
        
        player.showStatus();
        System.out.println();
        
        // Execute various commands
        System.out.println("1. Playing content and adjusting settings:");
        remote.playContent("Stranger Things S4");
        remote.adjustVolume(75);
        remote.seekTo(300);
        remote.toggleFullscreen();
        
        player.showStatus();
        remote.showHistory();
        
        // Test undo functionality
        System.out.println("\n2. Testing undo operations:");
        remote.undo(); // Undo fullscreen
        remote.undo(); // Undo seek
        
        player.showStatus();
        
        // Test redo functionality
        System.out.println("\n3. Testing redo operations:");
        remote.redo(); // Redo seek
        remote.redo(); // Redo fullscreen
        
        player.showStatus();
        
        // Test macro commands
        System.out.println("\n4. Testing macro command (Theater Mode):");
        remote.createTheaterMode();
        
        player.showStatus();
        
        // Test shortcuts
        System.out.println("\n5. Testing shortcuts:");
        remote.saveShortcut("MOVIE_MODE", new MacroCommand("Movie Mode", Arrays.asList(
            new VolumeCommand(player, 90),
            new FullscreenCommand(player, true)
        )));
        
        remote.saveShortcut("QUICK_VOLUME", new VolumeCommand(player, 60));
        
        remote.showShortcuts();
        
        remote.executeShortcut("QUICK_VOLUME");
        remote.executeShortcut("MOVIE_MODE");
        
        player.showStatus();
        
        // Final history
        remote.showHistory();
        
        System.out.println("\n=== Command Pattern Benefits ===");
        System.out.println("✓ Decouples invoker from receiver");
        System.out.println("✓ Enables undo/redo functionality");
        System.out.println("✓ Supports macro commands for grouped operations");
        System.out.println("✓ Allows command queuing and logging");
        System.out.println("✓ Facilitates parameterization of objects with different requests");
    }
}
