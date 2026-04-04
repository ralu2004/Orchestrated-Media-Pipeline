package app.services.analysis;

import app.common.PipelineException;
import app.common.PipelineStage;
import app.model.JobRequest;

public class IntroOutroDetectorService implements PipelineStage<JobRequest, String> {
    @Override
    public String process(JobRequest input) throws PipelineException {
        return "00:00:42";
    }
}