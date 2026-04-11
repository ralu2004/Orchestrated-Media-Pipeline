package app.orchestrator;

import app.common.PipelineException;
import app.model.JobStatus;

/**
 * No-op progress reporter: does nothing. Useful when constructing an {@link Orchestrator} in tests.
 */
public final class NoOpProgressReporter implements ProgressReporter {

    @Override
    public void onStageStarted(JobStatus stage) {}

    @Override
    public void onStageCompleted(JobStatus stage, long elapsedMs) {}

    @Override
    public void onFailed(JobStatus stage, PipelineException cause) {}

    @Override
    public void onPipelineCompleted(long totalElapsedMs) {}
}
