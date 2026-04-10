package app.services.analysis;

import app.common.PipelineStage;
import app.model.AnalysisContext;
import app.model.AnalysisResult;

/**
 * Analysis phase: intro/outro, credits, and scene segmentation from precomputed ffmpeg logs.
 */
public interface AnalysisService extends PipelineStage<AnalysisContext, AnalysisResult> {}