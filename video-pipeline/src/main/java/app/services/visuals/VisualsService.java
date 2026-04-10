package app.services.visuals;

import app.common.PipelineStage;
import app.model.JobRequest;
import app.model.VisualsResult;

/**
 * Visuals phase: encoding profile, transcodes, and sprite generation.
 */
public interface VisualsService extends PipelineStage<JobRequest, VisualsResult> {
}
