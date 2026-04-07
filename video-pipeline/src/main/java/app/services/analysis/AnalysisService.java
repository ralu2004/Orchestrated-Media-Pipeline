package app.services.analysis;

import app.common.PipelineStage;
import app.model.AnalysisContext;
import app.model.AnalysisResult;

public interface AnalysisService extends PipelineStage<AnalysisContext, AnalysisResult> {}