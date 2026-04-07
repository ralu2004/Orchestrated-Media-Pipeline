package app.services.ingest;

import app.common.PipelineException;
import app.model.FormatInfo;
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
        try {
            String actualChecksum = integrityCheck.process(input);
            FormatInfo format = formatValidator.process(input);
            return new IngestResult(actualChecksum, format);
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Ingest phase failed", "INGESTING", e);
        }
    }
}
