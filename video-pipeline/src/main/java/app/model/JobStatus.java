package app.model;

/** Lifecycle states used by the orchestrator state machine. */
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
