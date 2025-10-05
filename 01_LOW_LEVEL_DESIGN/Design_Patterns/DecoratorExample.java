package com.systemdesign.patterns;

/**
 * Decorator Pattern Implementation
 * 
 * The Decorator pattern allows behavior to be added to objects dynamically
 * without altering their structure. Used extensively in Netflix's UI components
 * and middleware layers.
 * 
 * Real-world Netflix scenario: Video streaming with different quality layers,
 * subtitles, and audio tracks that can be dynamically added.
 */

// Base component interface
interface VideoStream {
    String getDescription();
    double getCost();
    void stream();
}

// Concrete component - Basic video stream
class BasicVideoStream implements VideoStream {
    @Override
    public String getDescription() {
        return "Basic video stream";
    }
    
    @Override
    public double getCost() {
        return 5.99;
    }
    
    @Override
    public void stream() {
        System.out.println("Streaming basic video...");
    }
}

// Abstract decorator
abstract class VideoStreamDecorator implements VideoStream {
    protected VideoStream videoStream;
    
    public VideoStreamDecorator(VideoStream videoStream) {
        this.videoStream = videoStream;
    }
    
    @Override
    public String getDescription() {
        return videoStream.getDescription();
    }
    
    @Override
    public double getCost() {
        return videoStream.getCost();
    }
    
    @Override
    public void stream() {
        videoStream.stream();
    }
}

// Concrete decorators
class HDQualityDecorator extends VideoStreamDecorator {
    public HDQualityDecorator(VideoStream videoStream) {
        super(videoStream);
    }
    
    @Override
    public String getDescription() {
        return videoStream.getDescription() + " + HD Quality";
    }
    
    @Override
    public double getCost() {
        return videoStream.getCost() + 2.00;
    }
    
    @Override
    public void stream() {
        videoStream.stream();
        System.out.println("Enhancing with HD quality...");
    }
}

class SubtitlesDecorator extends VideoStreamDecorator {
    private String language;
    
    public SubtitlesDecorator(VideoStream videoStream, String language) {
        super(videoStream);
        this.language = language;
    }
    
    @Override
    public String getDescription() {
        return videoStream.getDescription() + " + " + language + " Subtitles";
    }
    
    @Override
    public double getCost() {
        return videoStream.getCost() + 0.50;
    }
    
    @Override
    public void stream() {
        videoStream.stream();
        System.out.println("Adding " + language + " subtitles...");
    }
}

class AdFreeDecorator extends VideoStreamDecorator {
    public AdFreeDecorator(VideoStream videoStream) {
        super(videoStream);
    }
    
    @Override
    public String getDescription() {
        return videoStream.getDescription() + " + Ad-Free";
    }
    
    @Override
    public double getCost() {
        return videoStream.getCost() + 3.00;
    }
    
    @Override
    public void stream() {
        videoStream.stream();
        System.out.println("Blocking advertisements...");
    }
}

// Netflix-style streaming service
class StreamingService {
    public void processOrder(VideoStream stream) {
        System.out.println("Processing order for: " + stream.getDescription());
        System.out.println("Total cost: $" + stream.getCost());
        stream.stream();
        System.out.println("Order completed!\n");
    }
}

public class DecoratorExample {
    public static void main(String[] args) {
        System.out.println("=== Netflix-style Decorator Pattern Demo ===\n");
        
        StreamingService service = new StreamingService();
        
        // Basic stream
        VideoStream basicStream = new BasicVideoStream();
        service.processOrder(basicStream);
        
        // Enhanced stream with HD quality
        VideoStream hdStream = new HDQualityDecorator(new BasicVideoStream());
        service.processOrder(hdStream);
        
        // Premium stream with multiple enhancements
        VideoStream premiumStream = new AdFreeDecorator(
            new SubtitlesDecorator(
                new HDQualityDecorator(new BasicVideoStream()), 
                "Spanish"
            )
        );
        service.processOrder(premiumStream);
        
        // Ultra premium with all features
        VideoStream ultraStream = new SubtitlesDecorator(
            new AdFreeDecorator(
                new HDQualityDecorator(new BasicVideoStream())
            ), 
            "French"
        );
        service.processOrder(ultraStream);
        
        demonstrateFlexibility();
    }
    
    private static void demonstrateFlexibility() {
        System.out.println("=== Demonstrating Decorator Flexibility ===\n");
        
        // Different combinations for different user preferences
        VideoStream stream1 = new SubtitlesDecorator(
            new BasicVideoStream(), "German"
        );
        
        VideoStream stream2 = new AdFreeDecorator(
            new SubtitlesDecorator(new BasicVideoStream(), "Japanese")
        );
        
        System.out.println("Stream 1: " + stream1.getDescription() + " - $" + stream1.getCost());
        System.out.println("Stream 2: " + stream2.getDescription() + " - $" + stream2.getCost());
    }
}
