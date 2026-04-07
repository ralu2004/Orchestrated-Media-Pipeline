package app.services.compliance;

import app.common.PipelineException;
import app.model.ComplianceContext;
import app.model.ComplianceResult;

import java.util.List;

public class DefaultComplianceService implements ComplianceService {

    public DefaultComplianceService() {
    }

    @Override
    public ComplianceResult process(ComplianceContext input) throws PipelineException {
        return new ComplianceResult(
                List.of(),
                input.visualsResult().transcodedVideos()
        );
    }
}
