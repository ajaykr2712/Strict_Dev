import java.time.LocalDateTime;
import java.util.*;

/**
 * Builder Pattern Implementation for Complex Object Construction
 * 
 * Real-world Use Case: Netflix Content Configuration Builder
 * Building complex content configurations with many optional parameters
 */

public class BuilderExample {
    
    // Complex object to be built
    static class NetflixContent {
        private final String title;
        private final String contentType;
        private final String genre;
        private final int duration;
        private final String description;
        private final List<String> cast;
        private final List<String> directors;
        private final Map<String, String> metadata;
        private final boolean isOriginal;
        private final String ageRating;
        private final List<String> availableLanguages;
        private final Map<String, String> subtitles;
        private final String thumbnailUrl;
        private final LocalDateTime releaseDate;
        
        private NetflixContent(Builder builder) {
            this.title = builder.title;
            this.contentType = builder.contentType;
            this.genre = builder.genre;
            this.duration = builder.duration;
            this.description = builder.description;
            this.cast = new ArrayList<>(builder.cast);
            this.directors = new ArrayList<>(builder.directors);
            this.metadata = new HashMap<>(builder.metadata);
            this.isOriginal = builder.isOriginal;
            this.ageRating = builder.ageRating;
            this.availableLanguages = new ArrayList<>(builder.availableLanguages);
            this.subtitles = new HashMap<>(builder.subtitles);
            this.thumbnailUrl = builder.thumbnailUrl;
            this.releaseDate = builder.releaseDate;
        }
        
        // Getters
        public String getTitle() { return title; }
        public String getContentType() { return contentType; }
        public String getGenre() { return genre; }
        public int getDuration() { return duration; }
        public String getDescription() { return description; }
        public List<String> getCast() { return new ArrayList<>(cast); }
        public List<String> getDirectors() { return new ArrayList<>(directors); }
        public Map<String, String> getMetadata() { return new HashMap<>(metadata); }
        public boolean isOriginal() { return isOriginal; }
        public String getAgeRating() { return ageRating; }
        public List<String> getAvailableLanguages() { return new ArrayList<>(availableLanguages); }
        public Map<String, String> getSubtitles() { return new HashMap<>(subtitles); }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public LocalDateTime getReleaseDate() { return releaseDate; }
        
        // Builder class
        public static class Builder {
            // Required parameters
            private final String title;
            private final String contentType;
            
            // Optional parameters with default values
            private String genre = "General";
            private int duration = 0;
            private String description = "";
            private List<String> cast = new ArrayList<>();
            private List<String> directors = new ArrayList<>();
            private Map<String, String> metadata = new HashMap<>();
            private boolean isOriginal = false;
            private String ageRating = "NR";
            private List<String> availableLanguages = new ArrayList<>();
            private Map<String, String> subtitles = new HashMap<>();
            private String thumbnailUrl = "";
            private LocalDateTime releaseDate = LocalDateTime.now();
            
            public Builder(String title, String contentType) {
                this.title = title;
                this.contentType = contentType;
            }
            
            public Builder genre(String genre) {
                this.genre = genre;
                return this;
            }
            
            public Builder duration(int duration) {
                this.duration = duration;
                return this;
            }
            
            public Builder description(String description) {
                this.description = description;
                return this;
            }
            
            public Builder addCastMember(String actor) {
                this.cast.add(actor);
                return this;
            }
            
            public Builder cast(List<String> cast) {
                this.cast = new ArrayList<>(cast);
                return this;
            }
            
            public Builder addDirector(String director) {
                this.directors.add(director);
                return this;
            }
            
            public Builder directors(List<String> directors) {
                this.directors = new ArrayList<>(directors);
                return this;
            }
            
            public Builder addMetadata(String key, String value) {
                this.metadata.put(key, value);
                return this;
            }
            
            public Builder metadata(Map<String, String> metadata) {
                this.metadata = new HashMap<>(metadata);
                return this;
            }
            
            public Builder isOriginal(boolean isOriginal) {
                this.isOriginal = isOriginal;
                return this;
            }
            
            public Builder ageRating(String ageRating) {
                this.ageRating = ageRating;
                return this;
            }
            
            public Builder addLanguage(String language) {
                this.availableLanguages.add(language);
                return this;
            }
            
            public Builder languages(List<String> languages) {
                this.availableLanguages = new ArrayList<>(languages);
                return this;
            }
            
            public Builder addSubtitle(String language, String subtitleFile) {
                this.subtitles.put(language, subtitleFile);
                return this;
            }
            
            public Builder subtitles(Map<String, String> subtitles) {
                this.subtitles = new HashMap<>(subtitles);
                return this;
            }
            
            public Builder thumbnailUrl(String thumbnailUrl) {
                this.thumbnailUrl = thumbnailUrl;
                return this;
            }
            
            public Builder releaseDate(LocalDateTime releaseDate) {
                this.releaseDate = releaseDate;
                return this;
            }
            
            public NetflixContent build() {
                validate();
                return new NetflixContent(this);
            }
            
            private void validate() {
                if (title == null || title.trim().isEmpty()) {
                    throw new IllegalArgumentException("Title cannot be null or empty");
                }
                if (contentType == null || contentType.trim().isEmpty()) {
                    throw new IllegalArgumentException("Content type cannot be null or empty");
                }
                if (duration < 0) {
                    throw new IllegalArgumentException("Duration cannot be negative");
                }
            }
        }
        
        @Override
        public String toString() {
            return String.format(
                "NetflixContent{title='%s', type='%s', genre='%s', duration=%d, isOriginal=%s, ageRating='%s'}",
                title, contentType, genre, duration, isOriginal, ageRating
            );
        }
    }
    
    // Director class for method chaining with different builders
    static class NetflixContentDirector {
        
        public NetflixContent createStrangerThingsEpisode() {
            return new NetflixContent.Builder("Stranger Things S4E1", "TV_EPISODE")
                    .genre("Sci-Fi/Horror")
                    .duration(78)
                    .description("The Hellfire Club faces a new supernatural threat")
                    .addCastMember("Millie Bobby Brown")
                    .addCastMember("Finn Wolfhard")
                    .addCastMember("Gaten Matarazzo")
                    .addDirector("The Duffer Brothers")
                    .isOriginal(true)
                    .ageRating("TV-14")
                    .addLanguage("English")
                    .addLanguage("Spanish")
                    .addSubtitle("English", "en_subtitles.srt")
                    .addSubtitle("Spanish", "es_subtitles.srt")
                    .addMetadata("season", "4")
                    .addMetadata("episode", "1")
                    .addMetadata("series_id", "stranger_things")
                    .thumbnailUrl("https://netflix.com/thumbnails/st_s4e1.jpg")
                    .releaseDate(LocalDateTime.of(2022, 5, 27, 0, 0))
                    .build();
        }
        
        public NetflixContent createDocumentary() {
            return new NetflixContent.Builder("Our Planet", "DOCUMENTARY")
                    .genre("Nature/Documentary")
                    .duration(50)
                    .description("Stunning wildlife documentary narrated by David Attenborough")
                    .addDirector("Alastair Fothergill")
                    .addCastMember("David Attenborough")
                    .isOriginal(true)
                    .ageRating("TV-G")
                    .addLanguage("English")
                    .addLanguage("French")
                    .addLanguage("German")
                    .addSubtitle("English", "en_cc.srt")
                    .addSubtitle("French", "fr_subtitles.srt")
                    .addMetadata("narrator", "David Attenborough")
                    .addMetadata("production_company", "Silverback Films")
                    .thumbnailUrl("https://netflix.com/thumbnails/our_planet.jpg")
                    .build();
        }
        
        public NetflixContent createMovie() {
            return new NetflixContent.Builder("Red Notice", "MOVIE")
                    .genre("Action/Comedy")
                    .duration(118)
                    .description("An FBI profiler pursuing the world's most wanted art thief")
                    .addCastMember("Dwayne Johnson")
                    .addCastMember("Ryan Reynolds")
                    .addCastMember("Gal Gadot")
                    .addDirector("Rawson Marshall Thurber")
                    .isOriginal(true)
                    .ageRating("PG-13")
                    .addLanguage("English")
                    .addLanguage("Spanish")
                    .addLanguage("French")
                    .addSubtitle("English", "en_cc.srt")
                    .addSubtitle("Spanish", "es_subtitles.srt")
                    .addMetadata("budget", "$200M")
                    .addMetadata("box_office", "Netflix Original")
                    .thumbnailUrl("https://netflix.com/thumbnails/red_notice.jpg")
                    .build();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Builder Pattern Demo: Netflix Content Creation ===\n");
        
        NetflixContentDirector director = new NetflixContentDirector();
        
        // Create different types of content using the director
        NetflixContent episode = director.createStrangerThingsEpisode();
        NetflixContent documentary = director.createDocumentary();
        NetflixContent movie = director.createMovie();
        
        System.out.println("Created TV Episode:");
        System.out.println(episode);
        System.out.println("Cast: " + episode.getCast());
        System.out.println("Languages: " + episode.getAvailableLanguages());
        System.out.println();
        
        System.out.println("Created Documentary:");
        System.out.println(documentary);
        System.out.println("Directors: " + documentary.getDirectors());
        System.out.println();
        
        System.out.println("Created Movie:");
        System.out.println(movie);
        System.out.println("Metadata: " + movie.getMetadata());
        System.out.println();
        
        // Manual builder usage
        System.out.println("Manual Builder Usage:");
        NetflixContent customContent = new NetflixContent.Builder("The Crown", "TV_SERIES")
                .genre("Drama/Historical")
                .description("Historical drama about the British Royal Family")
                .isOriginal(true)
                .ageRating("TV-MA")
                .addLanguage("English")
                .build();
        
        System.out.println(customContent);
        
        System.out.println("\n=== Builder Pattern Benefits ===");
        System.out.println("✓ Handles complex object construction");
        System.out.println("✓ Makes optional parameters easy to manage");
        System.out.println("✓ Immutable objects once built");
        System.out.println("✓ Fluent interface for readable code");
        System.out.println("✓ Director pattern for common constructions");
    }
}
