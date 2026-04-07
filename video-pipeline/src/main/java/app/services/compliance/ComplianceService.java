package app.services.compliance;

import app.common.PipelineStage;
import app.model.ComplianceContext;
import app.model.ComplianceResult;

public interface ComplianceService extends PipelineStage<ComplianceContext, ComplianceResult> {
}
