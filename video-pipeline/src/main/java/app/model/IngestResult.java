package app.model;

public record IngestResult(boolean passed, String actualChecksum, FormatInfo formatInfo) {}