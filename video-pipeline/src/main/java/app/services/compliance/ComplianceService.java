package app.services.compliance;

import app.common.PipelineStage;
import app.model.ComplianceContext;
import app.model.ComplianceResult;

/** Stage contract for applying compliance checks and transformations to processed media. */
public interface ComplianceService extends PipelineStage<ComplianceContext, ComplianceResult> {
}
