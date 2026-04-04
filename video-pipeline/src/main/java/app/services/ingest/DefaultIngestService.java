package app.services.ingest;

import app.common.PipelineException;
import app.model.IngestResult;
import app.model.JobRequest;

public class DefaultIngestService implements IngestService {

    private final IntegrityCheckService integrityCheck;
    private final FormatValidatorService formatValidator;

    public DefaultIngestService(IntegrityCheckService integrityCheck, FormatValidatorService formatValidator) {
        this.integrityCheck = integrityCheck;
        this.formatValidator = formatValidator;
    }

    @Override
    public IngestResult process(JobRequest input) throws PipelineException {
        String actualChecksum = integrityCheck.process(input);
        String fileFormat = formatValidator.process(input);
        return new IngestResult(true, actualChecksum, fileFormat);
    }
}
