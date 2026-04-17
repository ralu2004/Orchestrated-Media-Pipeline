package app.services.compliance;

import app.common.PipelineException;
import app.model.ComplianceContext;
import app.model.ComplianceResult;

/** Default compliance stage that performs safety scanning and regional branding. */
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
