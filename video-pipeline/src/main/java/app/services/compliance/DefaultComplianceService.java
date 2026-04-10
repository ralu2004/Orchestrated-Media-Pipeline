package app.services.compliance;

import app.common.PipelineException;
import app.model.ComplianceContext;
import app.model.ComplianceResult;

/*
* Runs safety scanning then regional branding. 
*/
public class DefaultComplianceService implements ComplianceService {

    private final SafetyScannerService safetyScanner;
    private final RegionalBrandingService regionalBranding;

    public DefaultComplianceService(SafetyScannerService safetyScanner, RegionalBrandingService regionalBranding) {
        this.safetyScanner = safetyScanner;
        this.regionalBranding = regionalBranding;
    }

    @Override
    public ComplianceResult process(ComplianceContext input) throws PipelineException {
        return new ComplianceResult(safetyScanner.scan(input), regionalBranding.apply(input));
    }
}
