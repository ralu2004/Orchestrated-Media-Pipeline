package app.services.ingest;

import app.common.PipelineStage;
import app.model.IngestResult;
import app.model.JobRequest;

/**
 * Ingest phase: validate integrity and container format before downstream processing.
 */
public interface IngestService extends PipelineStage<JobRequest, IngestResult> {}