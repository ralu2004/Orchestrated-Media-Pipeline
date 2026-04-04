package app.services.ingest;

import app.common.PipelineStage;
import app.model.IngestResult;
import app.model.JobRequest;

public interface IngestService extends PipelineStage<JobRequest, IngestResult> {}