package app.orchestrator;

import app.model.JobStatus;

/**
 * Defines the legal {@link JobStatus} transitions for the pipeline.
 *
 * <p>Pipeline path: {@code PENDING → INGESTING → ANALYZING → PROCESSING → COMPLIANCE → PACKAGING → COMPLETED}.
 * From any non-terminal state, {@link JobStatus#FAILED} is allowed once.
 */
public final class Transitions {

    private Transitions() {}

    public static boolean isAllowed(JobStatus from, JobStatus to) {
        if (to == JobStatus.FAILED) {
            return from != JobStatus.COMPLETED && from != JobStatus.FAILED;
        }
        return switch (from) {
            case PENDING -> to == JobStatus.INGESTING;
            case INGESTING -> to == JobStatus.ANALYZING;
            case ANALYZING -> to == JobStatus.PROCESSING;
            case PROCESSING -> to == JobStatus.COMPLIANCE;
            case COMPLIANCE -> to == JobStatus.PACKAGING;
            case PACKAGING -> to == JobStatus.COMPLETED;
            default -> false;
        };
    }
}
