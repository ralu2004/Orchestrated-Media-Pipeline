package app.model;

/** Immutable request payload used to start a pipeline job. */
public record JobRequest(String jobId, String sourceFile, String expectedChecksum) {}