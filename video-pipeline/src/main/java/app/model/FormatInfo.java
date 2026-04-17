package app.model;

/** Basic media container metadata captured during ingest validation. */
public record FormatInfo(String fileFormat, double duration) {
    public FormatInfo {
        if (fileFormat == null || fileFormat.isBlank()) {
            throw new IllegalArgumentException("File format must not be blank");
        }
        if (duration < 0) {
            throw new IllegalArgumentException("Duration must be non-negative");
        }
    }
}
