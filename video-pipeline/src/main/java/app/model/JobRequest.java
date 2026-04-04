package app.model;

public record JobRequest(String jobId, String sourceFile, String expectedChecksum) {}