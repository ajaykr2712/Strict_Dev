/**
 * Factory Pattern Implementation
 * 
 * Use Case: Netflix Content Processing Factory
 * 
 * Netflix needs to process different types of content (movies, TV shows, documentaries)
 * with different encoding, metadata extraction, and thumbnail generation strategies.
 * The Factory pattern allows creating appropriate content processors without
 * specifying the exact class.
 * 
 * Real-world scenario: When Netflix receives new content uploads, the system
 * needs to automatically determine the content type and apply the appropriate
 * processing pipeline for encoding, quality variants, and metadata extraction.
 * 
 * @author System Design Expert
 * @version 1.0
 */

import java.time.Duration;
import java.util.*;

/**
 * Abstract Product - Content Processor Interface
 */
interface ContentProcessor {
    ProcessingResult processContent(ContentMetadata content);
    String getSupportedContentType();
    List<String> getSupportedFormats();
    Duration getEstimatedProcessingTime(ContentMetadata content);
}

/**
 * Content metadata representing uploaded content
 */
class ContentMetadata {
    private final String contentId;
    private final String title;
    private final String contentType;
    private final String originalFormat;
    private final long fileSizeBytes;
    private final Duration duration;
    private final Map<String, Object> metadata;
    
    public ContentMetadata(String contentId, String title, String contentType, 
                          String originalFormat, long fileSizeBytes, Duration duration) {
        this.contentId = contentId;
        this.title = title;
        this.contentType = contentType;
        this.originalFormat = originalFormat;
        this.fileSizeBytes = fileSizeBytes;
        this.duration = duration;
        this.metadata = new HashMap<>();
    }
    
    // Getters
    public String getContentId() { return contentId; }
    public String getTitle() { return title; }
    public String getContentType() { return contentType; }
    public String getOriginalFormat() { return originalFormat; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public Duration getDuration() { return duration; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }
}

/**
 * Processing result containing output information
 */
class ProcessingResult {
    private final String contentId;
    private final boolean success;
    private final List<String> generatedFiles;
    private final Map<String, String> processingMetadata;
    private final Duration processingTime;
    private final List<String> errors;
    
    public ProcessingResult(String contentId, boolean success, List<String> generatedFiles,
                           Map<String, String> processingMetadata, Duration processingTime,
                           List<String> errors) {
        this.contentId = contentId;
        this.success = success;
        this.generatedFiles = new ArrayList<>(generatedFiles);
        this.processingMetadata = new HashMap<>(processingMetadata);
        this.processingTime = processingTime;
        this.errors = new ArrayList<>(errors);
    }
    
    // Getters
    public String getContentId() { return contentId; }
    public boolean isSuccess() { return success; }
    public List<String> getGeneratedFiles() { return new ArrayList<>(generatedFiles); }
    public Map<String, String> getProcessingMetadata() { return new HashMap<>(processingMetadata); }
    public Duration getProcessingTime() { return processingTime; }
    public List<String> getErrors() { return new ArrayList<>(errors); }
}

/**
 * Concrete Product - Movie Content Processor
 */
class MovieContentProcessor implements ContentProcessor {
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("mp4", "mkv", "avi", "mov");
    
    @Override
    public ProcessingResult processContent(ContentMetadata content) {
        System.out.println("Processing movie: " + content.getTitle());
        
        long startTime = System.currentTimeMillis();
        List<String> generatedFiles = new ArrayList<>();
        Map<String, String> metadata = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        try {
            // Simulate movie processing steps
            
            // 1. Video encoding to multiple qualities
            generatedFiles.addAll(encodeMovieQualities(content));
            
            // 2. Generate movie thumbnails and posters
            generatedFiles.addAll(generateMovieArtwork(content));
            
            // 3. Extract movie metadata (genre, cast, etc.)
            extractMovieMetadata(content, metadata);
            
            // 4. Generate subtitles in multiple languages
            generatedFiles.addAll(generateSubtitles(content));
            
            metadata.put("content_type", "movie");
            metadata.put("processing_pipeline", "movie_standard");
            
        } catch (Exception e) {
            errors.add("Movie processing failed: " + e.getMessage());
        }
        
        long endTime = System.currentTimeMillis();
        Duration processingTime = Duration.ofMillis(endTime - startTime);
        
        return new ProcessingResult(
            content.getContentId(),
            errors.isEmpty(),
            generatedFiles,
            metadata,
            processingTime,
            errors
        );
    }
    
    private List<String> encodeMovieQualities(ContentMetadata content) {
        // Simulate encoding multiple video qualities
        System.out.println("  Encoding movie to multiple qualities...");
        return Arrays.asList(
            content.getContentId() + "_4k.mp4",
            content.getContentId() + "_1080p.mp4",
            content.getContentId() + "_720p.mp4",
            content.getContentId() + "_480p.mp4"
        );
    }
    
    private List<String> generateMovieArtwork(ContentMetadata content) {
        System.out.println("  Generating movie artwork...");
        return Arrays.asList(
            content.getContentId() + "_poster.jpg",
            content.getContentId() + "_backdrop.jpg",
            content.getContentId() + "_thumbnail.jpg"
        );
    }
    
    private void extractMovieMetadata(ContentMetadata content, Map<String, String> metadata) {
        System.out.println("  Extracting movie metadata...");
        metadata.put("duration", content.getDuration().toString());
        metadata.put("genre", "Action"); // Simulated
        metadata.put("rating", "PG-13"); // Simulated
    }
    
    private List<String> generateSubtitles(ContentMetadata content) {
        System.out.println("  Generating subtitles...");
        return Arrays.asList(
            content.getContentId() + "_en.srt",
            content.getContentId() + "_es.srt",
            content.getContentId() + "_fr.srt"
        );
    }
    
    @Override
    public String getSupportedContentType() {
        return "MOVIE";
    }
    
    @Override
    public List<String> getSupportedFormats() {
        return new ArrayList<>(SUPPORTED_FORMATS);
    }
    
    @Override
    public Duration getEstimatedProcessingTime(ContentMetadata content) {
        // Estimate based on file size: 1GB = 10 minutes processing
        long minutes = content.getFileSizeBytes() / (1024 * 1024 * 1024) * 10;
        return Duration.ofMinutes(Math.max(minutes, 5)); // Minimum 5 minutes
    }
}

/**
 * Concrete Product - TV Show Content Processor
 */
class TVShowContentProcessor implements ContentProcessor {
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("mp4", "mkv", "avi");
    
    @Override
    public ProcessingResult processContent(ContentMetadata content) {
        System.out.println("Processing TV show episode: " + content.getTitle());
        
        long startTime = System.currentTimeMillis();
        List<String> generatedFiles = new ArrayList<>();
        Map<String, String> metadata = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        try {
            // TV show specific processing
            
            // 1. Encode episode with binge-watching optimizations
            generatedFiles.addAll(encodeEpisodeQualities(content));
            
            // 2. Generate episode thumbnails and previews
            generatedFiles.addAll(generateEpisodeArtwork(content));
            
            // 3. Extract episode metadata (season, episode number, etc.)
            extractEpisodeMetadata(content, metadata);
            
            // 4. Generate episode recap and preview clips
            generatedFiles.addAll(generateEpisodeClips(content));
            
            // 5. Process for binge-watching features
            processBingeWatchingFeatures(content, metadata);
            
            metadata.put("content_type", "tv_episode");
            metadata.put("processing_pipeline", "tv_show_optimized");
            
        } catch (Exception e) {
            errors.add("TV show processing failed: " + e.getMessage());
        }
        
        long endTime = System.currentTimeMillis();
        Duration processingTime = Duration.ofMillis(endTime - startTime);
        
        return new ProcessingResult(
            content.getContentId(),
            errors.isEmpty(),
            generatedFiles,
            metadata,
            processingTime,
            errors
        );
    }
    
    private List<String> encodeEpisodeQualities(ContentMetadata content) {
        System.out.println("  Encoding TV episode with binge-optimization...");
        return Arrays.asList(
            content.getContentId() + "_1080p_optimized.mp4",
            content.getContentId() + "_720p_optimized.mp4",
            content.getContentId() + "_480p_mobile.mp4"
        );
    }
    
    private List<String> generateEpisodeArtwork(ContentMetadata content) {
        System.out.println("  Generating episode artwork...");
        return Arrays.asList(
            content.getContentId() + "_episode_thumb.jpg",
            content.getContentId() + "_scene_preview.jpg"
        );
    }
    
    private void extractEpisodeMetadata(ContentMetadata content, Map<String, String> metadata) {
        System.out.println("  Extracting episode metadata...");
        metadata.put("season", "1"); // Simulated
        metadata.put("episode", "5"); // Simulated
        metadata.put("series_id", "series_12345"); // Simulated
    }
    
    private List<String> generateEpisodeClips(ContentMetadata content) {
        System.out.println("  Generating episode clips...");
        return Arrays.asList(
            content.getContentId() + "_recap.mp4",
            content.getContentId() + "_preview.mp4"
        );
    }
    
    private void processBingeWatchingFeatures(ContentMetadata content, Map<String, String> metadata) {
        System.out.println("  Processing binge-watching features...");
        metadata.put("skip_intro_start", "00:00:30");
        metadata.put("skip_intro_end", "00:01:45");
        metadata.put("skip_credits_start", "00:42:30");
    }
    
    @Override
    public String getSupportedContentType() {
        return "TV_SHOW";
    }
    
    @Override
    public List<String> getSupportedFormats() {
        return new ArrayList<>(SUPPORTED_FORMATS);
    }
    
    @Override
    public Duration getEstimatedProcessingTime(ContentMetadata content) {
        // TV shows have additional processing for binge features
        long minutes = content.getFileSizeBytes() / (1024 * 1024 * 1024) * 15;
        return Duration.ofMinutes(Math.max(minutes, 8)); // Minimum 8 minutes
    }
}

/**
 * Concrete Product - Documentary Content Processor
 */
class DocumentaryContentProcessor implements ContentProcessor {
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("mp4", "mkv", "mov");
    
    @Override
    public ProcessingResult processContent(ContentMetadata content) {
        System.out.println("Processing documentary: " + content.getTitle());
        
        long startTime = System.currentTimeMillis();
        List<String> generatedFiles = new ArrayList<>();
        Map<String, String> metadata = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        try {
            // Documentary specific processing
            
            // 1. Encode with emphasis on quality over file size
            generatedFiles.addAll(encodeDocumentaryQualities(content));
            
            // 2. Generate educational content artwork
            generatedFiles.addAll(generateDocumentaryArtwork(content));
            
            // 3. Extract educational metadata
            extractDocumentaryMetadata(content, metadata);
            
            // 4. Generate chapter markers and educational clips
            generatedFiles.addAll(generateEducationalClips(content));
            
            // 5. Process accessibility features
            processAccessibilityFeatures(content, metadata);
            
            metadata.put("content_type", "documentary");
            metadata.put("processing_pipeline", "documentary_educational");
            
        } catch (Exception e) {
            errors.add("Documentary processing failed: " + e.getMessage());
        }
        
        long endTime = System.currentTimeMillis();
        Duration processingTime = Duration.ofMillis(endTime - startTime);
        
        return new ProcessingResult(
            content.getContentId(),
            errors.isEmpty(),
            generatedFiles,
            metadata,
            processingTime,
            errors
        );
    }
    
    private List<String> encodeDocumentaryQualities(ContentMetadata content) {
        System.out.println("  Encoding documentary with high quality settings...");
        return Arrays.asList(
            content.getContentId() + "_4k_hq.mp4",
            content.getContentId() + "_1080p_hq.mp4",
            content.getContentId() + "_720p_hq.mp4"
        );
    }
    
    private List<String> generateDocumentaryArtwork(ContentMetadata content) {
        System.out.println("  Generating educational artwork...");
        return Arrays.asList(
            content.getContentId() + "_educational_poster.jpg",
            content.getContentId() + "_topic_thumbnail.jpg"
        );
    }
    
    private void extractDocumentaryMetadata(ContentMetadata content, Map<String, String> metadata) {
        System.out.println("  Extracting educational metadata...");
        metadata.put("educational_category", "Science"); // Simulated
        metadata.put("age_rating", "E"); // Educational
        metadata.put("subtopic", "Climate Change"); // Simulated
    }
    
    private List<String> generateEducationalClips(ContentMetadata content) {
        System.out.println("  Generating educational clips and chapters...");
        return Arrays.asList(
            content.getContentId() + "_chapter_1.mp4",
            content.getContentId() + "_chapter_2.mp4",
            content.getContentId() + "_key_moments.mp4"
        );
    }
    
    private void processAccessibilityFeatures(ContentMetadata content, Map<String, String> metadata) {
        System.out.println("  Processing accessibility features...");
        metadata.put("audio_description", "available");
        metadata.put("closed_captions", "multiple_languages");
        metadata.put("sign_language", "asl_available");
    }
    
    @Override
    public String getSupportedContentType() {
        return "DOCUMENTARY";
    }
    
    @Override
    public List<String> getSupportedFormats() {
        return new ArrayList<>(SUPPORTED_FORMATS);
    }
    
    @Override
    public Duration getEstimatedProcessingTime(ContentMetadata content) {
        // Documentaries require more processing for educational features
        long minutes = content.getFileSizeBytes() / (1024 * 1024 * 1024) * 20;
        return Duration.ofMinutes(Math.max(minutes, 12)); // Minimum 12 minutes
    }
}

/**
 * Factory Interface
 */
interface ContentProcessorFactory {
    ContentProcessor createProcessor(String contentType);
    List<String> getSupportedContentTypes();
    boolean supportsContentType(String contentType);
}

/**
 * Concrete Factory - Netflix Content Processor Factory
 */
class NetflixContentProcessorFactory implements ContentProcessorFactory {
    
    // Registry of available processors
    private final Map<String, ContentProcessor> processors;
    
    public NetflixContentProcessorFactory() {
        processors = new HashMap<>();
        // Register all available processors
        registerProcessor(new MovieContentProcessor());
        registerProcessor(new TVShowContentProcessor());
        registerProcessor(new DocumentaryContentProcessor());
    }
    
    /**
     * Register a new content processor
     */
    private void registerProcessor(ContentProcessor processor) {
        processors.put(processor.getSupportedContentType(), processor);
    }
    
    /**
     * Factory method to create appropriate content processor
     */
    @Override
    public ContentProcessor createProcessor(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }
        
        String normalizedType = contentType.toUpperCase().trim();
        ContentProcessor processor = processors.get(normalizedType);
        
        if (processor == null) {
            throw new UnsupportedOperationException(
                "No processor available for content type: " + contentType + 
                ". Supported types: " + getSupportedContentTypes());
        }
        
        return processor;
    }
    
    @Override
    public List<String> getSupportedContentTypes() {
        return new ArrayList<>(processors.keySet());
    }
    
    @Override
    public boolean supportsContentType(String contentType) {
        return contentType != null && processors.containsKey(contentType.toUpperCase().trim());
    }
}

/**
 * Content Processing Service - Client code
 */
class NetflixContentProcessingService {
    private final ContentProcessorFactory processorFactory;
    
    public NetflixContentProcessingService(ContentProcessorFactory processorFactory) {
        this.processorFactory = processorFactory;
    }
    
    /**
     * Process content using appropriate processor
     */
    public ProcessingResult processContent(ContentMetadata content) {
        try {
            // Get appropriate processor using factory
            ContentProcessor processor = processorFactory.createProcessor(content.getContentType());
            
            // Log processing start
            System.out.println("Starting processing for content: " + content.getTitle() + 
                             " (Type: " + content.getContentType() + ")");
            System.out.println("Estimated processing time: " + 
                             processor.getEstimatedProcessingTime(content));
            
            // Process the content
            ProcessingResult result = processor.processContent(content);
            
            // Log result
            if (result.isSuccess()) {
                System.out.println("✅ Processing completed successfully");
                System.out.println("Generated files: " + result.getGeneratedFiles().size());
                System.out.println("Actual processing time: " + result.getProcessingTime());
            } else {
                System.out.println("❌ Processing failed with errors:");
                result.getErrors().forEach(error -> System.out.println("  - " + error));
            }
            
            return result;
            
        } catch (Exception e) {
            System.out.println("❌ Processing failed: " + e.getMessage());
            return new ProcessingResult(
                content.getContentId(),
                false,
                Collections.emptyList(),
                Collections.emptyMap(),
                Duration.ZERO,
                Arrays.asList(e.getMessage())
            );
        }
    }
    
    /**
     * Get supported content types
     */
    public List<String> getSupportedContentTypes() {
        return processorFactory.getSupportedContentTypes();
    }
}

/**
 * Demonstration class showing Factory pattern usage
 */
public class FactoryExample {
    public static void main(String[] args) {
        System.out.println("=== Netflix Content Processing Factory Demo ===\n");
        
        // Create factory and processing service
        ContentProcessorFactory factory = new NetflixContentProcessorFactory();
        NetflixContentProcessingService service = new NetflixContentProcessingService(factory);
        
        // Show supported content types
        System.out.println("Supported content types: " + service.getSupportedContentTypes());
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Create sample content for different types
        List<ContentMetadata> contentList = createSampleContent();
        
        // Process each content item
        for (ContentMetadata content : contentList) {
            ProcessingResult result = service.processContent(content);
            System.out.println("\n" + "-".repeat(40) + "\n");
            
            // Show processing details
            if (result.isSuccess()) {
                showProcessingDetails(result);
            }
        }
        
        // Demonstrate unsupported content type
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Testing unsupported content type:");
        try {
            ContentMetadata unsupportedContent = new ContentMetadata(
                "content_999", "Live Stream", "LIVE_STREAM", "rtmp", 0, Duration.ofHours(2)
            );
            service.processContent(unsupportedContent);
        } catch (Exception e) {
            System.out.println("Expected error: " + e.getMessage());
        }
    }
    
    private static List<ContentMetadata> createSampleContent() {
        List<ContentMetadata> content = new ArrayList<>();
        
        // Movie content
        ContentMetadata movie = new ContentMetadata(
            "movie_001",
            "The Matrix Resurrections",
            "MOVIE",
            "mp4",
            4L * 1024 * 1024 * 1024, // 4GB
            Duration.ofMinutes(148)
        );
        movie.addMetadata("director", "Lana Wachowski");
        movie.addMetadata("year", "2021");
        content.add(movie);
        
        // TV Show episode
        ContentMetadata tvShow = new ContentMetadata(
            "episode_002",
            "Stranger Things S4E1",
            "TV_SHOW",
            "mkv",
            2L * 1024 * 1024 * 1024, // 2GB
            Duration.ofMinutes(78)
        );
        tvShow.addMetadata("series", "Stranger Things");
        tvShow.addMetadata("season", "4");
        content.add(tvShow);
        
        // Documentary
        ContentMetadata documentary = new ContentMetadata(
            "doc_003",
            "Our Planet: Frozen Worlds",
            "DOCUMENTARY",
            "mov",
            3L * 1024 * 1024 * 1024, // 3GB
            Duration.ofMinutes(50)
        );
        documentary.addMetadata("narrator", "David Attenborough");
        documentary.addMetadata("topic", "Climate Science");
        content.add(documentary);
        
        return content;
    }
    
    private static void showProcessingDetails(ProcessingResult result) {
        System.out.println("Processing Details:");
        System.out.println("  Content ID: " + result.getContentId());
        System.out.println("  Success: " + result.isSuccess());
        System.out.println("  Processing Time: " + result.getProcessingTime());
        System.out.println("  Generated Files: " + result.getGeneratedFiles().size());
        
        if (!result.getGeneratedFiles().isEmpty()) {
            System.out.println("  Sample Generated Files:");
            result.getGeneratedFiles().stream()
                   .limit(3)
                   .forEach(file -> System.out.println("    - " + file));
            if (result.getGeneratedFiles().size() > 3) {
                System.out.println("    ... and " + (result.getGeneratedFiles().size() - 3) + " more");
            }
        }
        
        if (!result.getProcessingMetadata().isEmpty()) {
            System.out.println("  Metadata:");
            result.getProcessingMetadata().entrySet().stream()
                   .limit(3)
                   .forEach(entry -> System.out.println("    " + entry.getKey() + ": " + entry.getValue()));
        }
    }
}
