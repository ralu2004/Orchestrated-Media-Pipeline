package app.orchestrator;

import app.common.PipelineException;
import app.model.*;

public class PipelineJob {

    private JobStatus status;
    private PipelineException failureCause;
    private JobRequest jobRequest;
    private IngestResult ingestResult;
    private AnalysisResult analysisResult;
    private VisualsResult visualsResult;
    private AudioResult audioResult;
    private ComplianceResult complianceResult;
    private PackagingResult packagingResult;

    PipelineJob(JobRequest request) {
        this.jobRequest = request;
        this.status = JobStatus.PENDING;
    }

    public JobStatus getStatus() {
        return status;
    }

    public PipelineException getFailureCause() {
        return failureCause;
    }

    public JobRequest getJobRequest() {
        return jobRequest;
    }

    public IngestResult getIngestResult() {
        return ingestResult;
    }

    public AnalysisResult getAnalysisResult() {
        return analysisResult;
    }

    public VisualsResult getVisualsResult() {
        return visualsResult;
    }

    public AudioResult getAudioResult() {
        return audioResult;
    }

    public ComplianceResult getComplianceResult() {
        return complianceResult;
    }

    public PackagingResult getPackagingResult() {
        return packagingResult;
    }

    void setStatus(JobStatus status) {
        this.status = status;
    }

    void setFailureCause(PipelineException cause) {
        this.failureCause = cause;
    }

    void setPackagingResult(PackagingResult packagingResult) {
        this.packagingResult = packagingResult;
    }

    void setComplianceResult(ComplianceResult complianceResult) {
        this.complianceResult = complianceResult;
    }

    void setAudioResult(AudioResult audioResult) {
        this.audioResult = audioResult;
    }

    void setVisualsResult(VisualsResult visualsResult) {
        this.visualsResult = visualsResult;
    }

    void setAnalysisResult(AnalysisResult analysisResult) {
        this.analysisResult = analysisResult;
    }

    void setIngestResult(IngestResult ingestResult) {
        this.ingestResult = ingestResult;
    }
}
