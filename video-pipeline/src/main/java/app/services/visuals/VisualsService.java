package app.services.visuals;

import app.common.PipelineStage;
import app.model.JobRequest;
import app.model.VisualsResult;

public interface VisualsService extends PipelineStage<JobRequest, VisualsResult> {
}
