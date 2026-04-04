package app.services.analysis;

import app.common.PipelineStage;
import app.model.AnalysisResult;
import app.model.JobRequest;

public interface AnalysisService extends PipelineStage<JobRequest, AnalysisResult> {}