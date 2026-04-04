package app.services.ingest;

import app.common.PipelineException;
import app.common.PipelineStage;
import app.model.JobRequest;

public class IntegrityCheckService implements PipelineStage<JobRequest, String> {
    @Override
    public String process(JobRequest input) throws PipelineException {
        return "stub-checksum-abc123";
    }
}
