package app.orchestrator;

import app.common.PipelineException;
import app.model.JobStatus;

/**
 * Reports pipeline progress driven by {@link Orchestrator}: each {@link JobStatus} work
 * phase (ingest through packaging), terminal success via {@link #onPipelineCompleted(long)}, and failures.
 */
public interface ProgressReporter {

    void onStageStarted(JobStatus stage);

    void onStageCompleted(JobStatus stage, long elapsedMs);

    void onFailed(JobStatus stage, PipelineException cause);

    void onPipelineCompleted(long totalElapsedMs);
}
