package app.orchestrator;

import app.common.PipelineException;
import app.common.PipelineStageName;
import app.model.*;
import app.services.analysis.AnalysisService;
import app.services.audio.AudioService;
import app.services.compliance.ComplianceService;
import app.services.ingest.IngestService;
import app.services.packaging.PackagingService;
import app.services.visuals.VisualsService;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * The state machine mediating the workflow. It runs the media pipeline and drives {@link PipelineJob}
 * through explicit {@link JobStatus} transitions ({@link Transitions}).
 */
public class Orchestrator {

    private final IngestService ingestService;
    private final AnalysisService analysisService;
    private final VisualsService visualsService;
    private final AudioService audioService;
    private final ComplianceService complianceService;
    private final PackagingService packagingService;
    private final ExecutorService executor;
    private final ProgressReporter reporter;

    public Orchestrator(IngestService ingestService,
                        AnalysisService analysisService,
                        VisualsService visualsService,
                        AudioService audioService,
                        ComplianceService complianceService,
                        PackagingService packagingService,
                        ExecutorService executor,
                        ProgressReporter reporter) {
        this.ingestService = ingestService;
        this.analysisService = analysisService;
        this.visualsService = visualsService;
        this.audioService = audioService;
        this.complianceService = complianceService;
        this.packagingService = packagingService;
        this.executor = executor;
        this.reporter = Objects.requireNonNull(reporter);
    }

    public PipelineJob run(JobRequest request) {
        PipelineJob job = new PipelineJob(request);

        try {
            long pipelineStart = System.currentTimeMillis();

            // INGESTING
            job.applyTransition(JobStatus.INGESTING);
            long ingestStart = System.currentTimeMillis();
            reporter.onStageStarted(JobStatus.INGESTING);
            IngestResult ingestResult = ingestService.process(request);
            job.setIngestResult(ingestResult);
            reporter.onStageCompleted(JobStatus.INGESTING, System.currentTimeMillis() - ingestStart);

            // ANALYZING
            job.applyTransition(JobStatus.ANALYZING);
            long analysisStart = System.currentTimeMillis();
            reporter.onStageStarted(JobStatus.ANALYZING);
            AnalysisResult analysisResult = analysisService.process(new AnalysisContext(request, ingestResult));
            job.setAnalysisResult(analysisResult);
            reporter.onStageCompleted(JobStatus.ANALYZING, System.currentTimeMillis() - analysisStart);

            // PROCESSING (visuals || audio)
            job.applyTransition(JobStatus.PROCESSING);
            long processingStart = System.currentTimeMillis();
            reporter.onStageStarted(JobStatus.PROCESSING);
            CompletableFuture<VisualsResult> visualsFuture = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return visualsService.process(request);
                        } catch (PipelineException e) {
                            throw new RuntimeException(e);
                        }
                    }, executor);
            CompletableFuture<AudioResult> audioFuture = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return audioService.process(new AudioContext(request, analysisResult));
                        } catch (PipelineException e) {
                            throw new RuntimeException(e);
                        }
                    }, executor);

            CompletableFuture.allOf(visualsFuture, audioFuture).join();

            VisualsResult visualsResult = getFutureResult(visualsFuture);
            AudioResult audioResult = getFutureResult(audioFuture);
            job.setVisualsResult(visualsResult);
            job.setAudioResult(audioResult);
            reporter.onStageCompleted(JobStatus.PROCESSING, System.currentTimeMillis() - processingStart);

            // COMPLIANCE
            job.applyTransition(JobStatus.COMPLIANCE);
            long complianceStart = System.currentTimeMillis();
            reporter.onStageStarted(JobStatus.COMPLIANCE);
            ComplianceResult complianceResult = complianceService.process(new ComplianceContext(request, visualsResult));
            job.setComplianceResult(complianceResult);
            reporter.onStageCompleted(JobStatus.COMPLIANCE, System.currentTimeMillis() - complianceStart);

            // PACKAGING
            job.applyTransition(JobStatus.PACKAGING);
            long packagingStart = System.currentTimeMillis();
            reporter.onStageStarted(JobStatus.PACKAGING);
            PackagingResult packagingResult = packagingService.process(new PackagingContext(request, visualsResult, audioResult, complianceResult));
            job.setPackagingResult(packagingResult);
            reporter.onStageCompleted(JobStatus.PACKAGING, System.currentTimeMillis() - packagingStart);

            // COMPLETED
            job.applyTransition(JobStatus.COMPLETED);
            reporter.onPipelineCompleted(System.currentTimeMillis() - pipelineStart);

        } catch (PipelineException e) {
            reporter.onFailed(job.getStatus(), e);
            safeTransitionToFailed(job);
            job.setFailureCause(e);
        } catch (Exception e) {
            PipelineException pe = new PipelineException("Unexpected error", PipelineStageName.UNKNOWN, e);
            reporter.onFailed(job.getStatus(), pe);
            safeTransitionToFailed(job);
            job.setFailureCause(pe);
        }

        return job;
    }

    private static void safeTransitionToFailed(PipelineJob job) {
        try {
            job.applyTransition(JobStatus.FAILED);
        } catch (PipelineException ignored) {
            // already in FAILED or COMPLETED
        }
    }

    private <T> T getFutureResult(CompletableFuture<T> future) throws PipelineException {
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof PipelineException pe) {
                throw pe;
            }
            if (cause instanceof RuntimeException re && re.getCause() instanceof PipelineException pe) {
                throw pe;
            }
            throw new PipelineException("Parallel processing failed", PipelineStageName.PROCESSING, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PipelineException("Parallel processing interrupted", PipelineStageName.PROCESSING, e);
        }
    }
}
