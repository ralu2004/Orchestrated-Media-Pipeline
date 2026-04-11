package app.orchestrator;

import app.common.PipelineException;
import app.common.PipelineStageName;
import app.model.*;

/**
 * Mutable snapshot of a single pipeline run: current {@link JobStatus}, optional {@link PipelineException}
 * on failure, and phase outputs as they become available.
 *
 * <p>Constructed by {@link Orchestrator} with status {@link JobStatus#PENDING}. Status changes must go
 * through {@link #applyTransition(JobStatus)} so they stay consistent with {@link Transitions}.
 * 
 * Package-private setters below exist only for {@link Orchestrator} to attach phase outputs.
 */
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

    /** Starts in {@link JobStatus#PENDING}; only {@link Orchestrator} should construct instances. */
    PipelineJob(JobRequest request) {
        this.jobRequest = request;
        this.status = JobStatus.PENDING;
    }

    /** Current lifecycle state of the job. */
    public JobStatus getStatus() {
        return status;
    }

    /**
     * When {@link #getStatus()} is {@link JobStatus#FAILED}, the error that stopped the pipeline;
     * otherwise typically {@code null}.
     */
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

    /**
     * Moves to {@code next} only if {@link Transitions#isAllowed(JobStatus, JobStatus)} permits it;
     * otherwise throws with stage {@code ORCHESTRATION}.
     */
    void applyTransition(JobStatus next) throws PipelineException {
        if (!Transitions.isAllowed(status, next)) {
            throw new PipelineException("Illegal pipeline transition: " + status + " -> " + next, PipelineStageName.ORCHESTRATION);
        }
        this.status = next;
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
