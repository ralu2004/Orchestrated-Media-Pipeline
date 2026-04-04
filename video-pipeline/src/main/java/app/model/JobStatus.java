package app.model;

public enum JobStatus {
    PENDING,
    INGESTING,
    ANALYZING,
    PROCESSING, // stages 3 + 4 running in parallel
    COMPLIANCE,
    PACKAGING,
    COMPLETED,
    FAILED
}
