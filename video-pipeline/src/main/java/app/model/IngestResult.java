package app.model;

public record IngestResult(String actualChecksum, FormatInfo formatInfo) {
    public IngestResult {
        if (actualChecksum == null || actualChecksum.isBlank()) {
            throw new IllegalArgumentException("Actual checksum must not be blank");
        }
        if (formatInfo == null) {
            throw new IllegalArgumentException("Format info must not be null");
        }
    }
}