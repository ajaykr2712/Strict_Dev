import java.util.*;
import java.util.concurrent.*;

/**
 * Memento Pattern Implementation
 * 
 * Real-world Use Case: Netflix Watch History and Resume Functionality
 * Saves and restores user viewing state across sessions
 */

public class MementoExample {
    
    // Memento interface
    interface ViewingStateMemento {
        // Intentionally no public methods - only originator can access state
    }
    
    // Originator - Netflix Video Player
    static class NetflixVideoPlayer {
        private String contentId;
        private String contentTitle;
        private int currentPosition; // seconds
        private int totalDuration; // seconds
        private String quality;
        private boolean isPlaying;
        private int volume;
        private boolean subtitlesEnabled;
        private String subtitleLanguage;
        private double playbackSpeed;
        private boolean isFullscreen;
        
        public NetflixVideoPlayer() {
            this.volume = 50;
            this.playbackSpeed = 1.0;
            this.quality = "HD";
            this.subtitleLanguage = "English";
        }
        
        // Memento implementation (inner class has access to outer class state)
        private class ConcreteViewingStateMemento implements ViewingStateMemento {
            private final String contentId;
            private final String contentTitle;
            private final int currentPosition;
            private final int totalDuration;
            private final String quality;
            private final boolean isPlaying;
            private final int volume;
            private final boolean subtitlesEnabled;
            private final String subtitleLanguage;
            private final double playbackSpeed;
            private final boolean isFullscreen;
            private final long timestamp;
            
            private ConcreteViewingStateMemento() {
                this.contentId = NetflixVideoPlayer.this.contentId;
                this.contentTitle = NetflixVideoPlayer.this.contentTitle;
                this.currentPosition = NetflixVideoPlayer.this.currentPosition;
                this.totalDuration = NetflixVideoPlayer.this.totalDuration;
                this.quality = NetflixVideoPlayer.this.quality;
                this.isPlaying = NetflixVideoPlayer.this.isPlaying;
                this.volume = NetflixVideoPlayer.this.volume;
                this.subtitlesEnabled = NetflixVideoPlayer.this.subtitlesEnabled;
                this.subtitleLanguage = NetflixVideoPlayer.this.subtitleLanguage;
                this.playbackSpeed = NetflixVideoPlayer.this.playbackSpeed;
                this.isFullscreen = NetflixVideoPlayer.this.isFullscreen;
                this.timestamp = System.currentTimeMillis();
            }
        }
        
        // Create memento
        public ViewingStateMemento saveState() {
            System.out.println("[PLAYER] Saving viewing state for: " + contentTitle + 
                             " at position " + formatTime(currentPosition));
            return new ConcreteViewingStateMemento();
        }
        
        // Restore from memento
        public void restoreState(ViewingStateMemento memento) {
            if (memento instanceof ConcreteViewingStateMemento) {
                ConcreteViewingStateMemento concrete = (ConcreteViewingStateMemento) memento;
                
                this.contentId = concrete.contentId;
                this.contentTitle = concrete.contentTitle;
                this.currentPosition = concrete.currentPosition;
                this.totalDuration = concrete.totalDuration;
                this.quality = concrete.quality;
                this.isPlaying = concrete.isPlaying;
                this.volume = concrete.volume;
                this.subtitlesEnabled = concrete.subtitlesEnabled;
                this.subtitleLanguage = concrete.subtitleLanguage;
                this.playbackSpeed = concrete.playbackSpeed;
                this.isFullscreen = concrete.isFullscreen;
                
                System.out.println("[PLAYER] Restored viewing state for: " + contentTitle + 
                                 " at position " + formatTime(currentPosition));
            }
        }
        
        // Player operations
        public void loadContent(String contentId, String title, int duration) {
            this.contentId = contentId;
            this.contentTitle = title;
            this.totalDuration = duration;
            this.currentPosition = 0;
            this.isPlaying = false;
            System.out.println("[PLAYER] Loaded: " + title + " (" + formatTime(duration) + ")");
        }
        
        public void play() {
            this.isPlaying = true;
            System.out.println("[PLAYER] Playing: " + contentTitle);
        }
        
        public void pause() {
            this.isPlaying = false;
            System.out.println("[PLAYER] Paused: " + contentTitle);
        }
        
        public void seek(int position) {
            this.currentPosition = Math.max(0, Math.min(totalDuration, position));
            System.out.println("[PLAYER] Seeked to: " + formatTime(currentPosition));
        }
        
        public void setVolume(int volume) {
            this.volume = Math.max(0, Math.min(100, volume));
            System.out.println("[PLAYER] Volume: " + this.volume);
        }
        
        public void setQuality(String quality) {
            this.quality = quality;
            System.out.println("[PLAYER] Quality: " + quality);
        }
        
        public void enableSubtitles(boolean enabled, String language) {
            this.subtitlesEnabled = enabled;
            if (enabled) {
                this.subtitleLanguage = language;
            }
            System.out.println("[PLAYER] Subtitles: " + (enabled ? "ON (" + language + ")" : "OFF"));
        }
        
        public void setPlaybackSpeed(double speed) {
            this.playbackSpeed = speed;
            System.out.println("[PLAYER] Playback speed: " + speed + "x");
        }
        
        public void toggleFullscreen() {
            this.isFullscreen = !this.isFullscreen;
            System.out.println("[PLAYER] Fullscreen: " + (isFullscreen ? "ON" : "OFF"));
        }
        
        // Simulate playback progress
        public void simulatePlayback(int seconds) {
            if (isPlaying) {
                currentPosition += seconds;
                if (currentPosition >= totalDuration) {
                    currentPosition = totalDuration;
                    isPlaying = false;
                    System.out.println("[PLAYER] Finished playing: " + contentTitle);
                } else {
                    System.out.println("[PLAYER] Progress: " + formatTime(currentPosition) + 
                                     " / " + formatTime(totalDuration));
                }
            }
        }
        
        private String formatTime(int seconds) {
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            int secs = seconds % 60;
            if (hours > 0) {
                return String.format("%d:%02d:%02d", hours, minutes, secs);
            } else {
                return String.format("%d:%02d", minutes, secs);
            }
        }
        
        public void showStatus() {
            System.out.println("\n[PLAYER STATUS]");
            System.out.println("  Content: " + (contentTitle != null ? contentTitle : "None"));
            System.out.println("  Position: " + formatTime(currentPosition) + " / " + formatTime(totalDuration));
            System.out.println("  Status: " + (isPlaying ? "Playing" : "Paused"));
            System.out.println("  Volume: " + volume);
            System.out.println("  Quality: " + quality);
            System.out.println("  Subtitles: " + (subtitlesEnabled ? "ON (" + subtitleLanguage + ")" : "OFF"));
            System.out.println("  Speed: " + playbackSpeed + "x");
            System.out.println("  Fullscreen: " + (isFullscreen ? "ON" : "OFF"));
        }
        
        // Getters for external access
        public String getContentId() { return contentId; }
        public String getContentTitle() { return contentTitle; }
        public int getCurrentPosition() { return currentPosition; }
        public boolean isPlaying() { return isPlaying; }
    }
    
    // Caretaker - Watch History Manager
    static class WatchHistoryManager {
        private final Map<String, List<ViewingStateMemento>> userHistory;
        private final Map<String, ViewingStateMemento> currentSessions;
        private final int maxHistoryPerUser;
        
        public WatchHistoryManager(int maxHistoryPerUser) {
            this.userHistory = new ConcurrentHashMap<>();
            this.currentSessions = new ConcurrentHashMap<>();
            this.maxHistoryPerUser = maxHistoryPerUser;
        }
        
        public void saveViewingState(String userId, NetflixVideoPlayer player) {
            ViewingStateMemento memento = player.saveState();
            
            // Save current session
            currentSessions.put(userId, memento);
            
            // Add to history
            List<ViewingStateMemento> history = userHistory.computeIfAbsent(userId, 
                    k -> new ArrayList<>());
            
            // Maintain history size limit
            if (history.size() >= maxHistoryPerUser) {
                history.remove(0); // Remove oldest
            }
            
            history.add(memento);
            
            System.out.println("[HISTORY] Saved viewing state for user: " + userId);
        }
        
        public boolean restoreCurrentSession(String userId, NetflixVideoPlayer player) {
            ViewingStateMemento memento = currentSessions.get(userId);
            if (memento != null) {
                player.restoreState(memento);
                System.out.println("[HISTORY] Restored current session for user: " + userId);
                return true;
            }
            System.out.println("[HISTORY] No current session found for user: " + userId);
            return false;
        }
        
        public boolean restoreFromHistory(String userId, int historyIndex, NetflixVideoPlayer player) {
            List<ViewingStateMemento> history = userHistory.get(userId);
            if (history != null && historyIndex >= 0 && historyIndex < history.size()) {
                ViewingStateMemento memento = history.get(historyIndex);
                player.restoreState(memento);
                System.out.println("[HISTORY] Restored from history index " + historyIndex + 
                                 " for user: " + userId);
                return true;
            }
            System.out.println("[HISTORY] Invalid history index for user: " + userId);
            return false;
        }
        
        public void clearSession(String userId) {
            currentSessions.remove(userId);
            System.out.println("[HISTORY] Cleared current session for user: " + userId);
        }
        
        public void clearHistory(String userId) {
            userHistory.remove(userId);
            System.out.println("[HISTORY] Cleared all history for user: " + userId);
        }
        
        public int getHistorySize(String userId) {
            List<ViewingStateMemento> history = userHistory.get(userId);
            return history != null ? history.size() : 0;
        }
        
        public void showUserHistory(String userId) {
            List<ViewingStateMemento> history = userHistory.get(userId);
            System.out.println("\n[HISTORY] User " + userId + " viewing history:");
            if (history == null || history.isEmpty()) {
                System.out.println("  No viewing history");
            } else {
                for (int i = 0; i < history.size(); i++) {
                    System.out.println("  " + i + ". Viewing state saved");
                }
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Memento Pattern Demo: Netflix Watch History ===\n");
        
        NetflixVideoPlayer player = new NetflixVideoPlayer();
        WatchHistoryManager historyManager = new WatchHistoryManager(5);
        
        String userId = "user123";
        
        // User starts watching content
        System.out.println("1. User starts watching Stranger Things:");
        player.loadContent("st_s4_e1", "Stranger Things S4E1", 3600); // 1 hour
        player.play();
        player.setVolume(75);
        player.enableSubtitles(true, "English");
        player.simulatePlayback(1200); // Watch for 20 minutes
        
        player.showStatus();
        
        // Save state when user pauses
        player.pause();
        historyManager.saveViewingState(userId, player);
        
        System.out.println("\n2. User switches to different content:");
        player.loadContent("crown_s5_e3", "The Crown S5E3", 3300); // 55 minutes
        player.play();
        player.setQuality("4K");
        player.setPlaybackSpeed(1.25);
        player.simulatePlayback(600); // Watch for 10 minutes
        
        player.showStatus();
        
        // Save state again
        player.pause();
        historyManager.saveViewingState(userId, player);
        
        System.out.println("\n3. User watches another show:");
        player.loadContent("squid_game_e1", "Squid Game E1", 3420); // 57 minutes
        player.play();
        player.toggleFullscreen();
        player.enableSubtitles(false, "");
        player.simulatePlayback(900); // Watch for 15 minutes
        
        player.showStatus();
        historyManager.saveViewingState(userId, player);
        
        // Show history
        historyManager.showUserHistory(userId);
        
        System.out.println("\n4. User logs out and back in - restore current session:");
        player = new NetflixVideoPlayer(); // Simulate new session
        
        boolean restored = historyManager.restoreCurrentSession(userId, player);
        if (restored) {
            player.showStatus();
        }
        
        System.out.println("\n5. User wants to continue previous show (history index 1):");
        historyManager.restoreFromHistory(userId, 1, player);
        player.showStatus();
        
        // Continue watching
        player.play();
        player.simulatePlayback(300); // Watch 5 more minutes
        player.showStatus();
        
        System.out.println("\n6. Save multiple states to demonstrate history limit:");
        for (int i = 1; i <= 7; i++) {
            player.seek(i * 300); // Different positions
            historyManager.saveViewingState(userId, player);
            Thread.sleep(100); // Small delay to ensure different timestamps
        }
        
        historyManager.showUserHistory(userId);
        System.out.println("History size: " + historyManager.getHistorySize(userId) + " (max: 5)");
        
        // Test restoring from different history points
        System.out.println("\n7. Restore from different history points:");
        historyManager.restoreFromHistory(userId, 0, player); // Oldest in current history
        System.out.println("Oldest state position: " + player.getCurrentPosition() + " seconds");
        
        historyManager.restoreFromHistory(userId, 4, player); // Newest in history
        System.out.println("Newest state position: " + player.getCurrentPosition() + " seconds");
        
        System.out.println("\n=== Memento Pattern Benefits ===");
        System.out.println("✓ Captures and externalizes object state without violating encapsulation");
        System.out.println("✓ Enables undo/redo and state restoration functionality");
        System.out.println("✓ Simplifies originator by delegating state management to caretaker");
        System.out.println("✓ Supports multiple snapshots and selective restoration");
        System.out.println("✓ Preserves object integrity while allowing state manipulation");
    }
}
