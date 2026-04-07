package app.orchestrator;

import app.common.PipelineException;
import app.model.*;
import app.services.analysis.AnalysisService;
import app.services.audio.AudioService;
import app.services.compliance.ComplianceService;
import app.services.ingest.IngestService;
import app.services.packaging.PackagingService;
import app.services.visuals.VisualsService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class Orchestrator {

    private final IngestService ingestService;
    private final AnalysisService analysisService;
    private final VisualsService visualsService;
    private final AudioService audioService;
    private final ComplianceService complianceService;
    private final PackagingService packagingService;
    private final ExecutorService executor;

    public Orchestrator(IngestService ingestService,
                        AnalysisService analysisService,
                        VisualsService visualsService,
                        AudioService audioService,
                        ComplianceService complianceService,
                        PackagingService packagingService,
                        ExecutorService executor) {
        this.ingestService = ingestService;
        this.analysisService = analysisService;
        this.visualsService = visualsService;
        this.audioService = audioService;
        this.complianceService = complianceService;
        this.packagingService = packagingService;
        this.executor = executor;
    }

    public PipelineJob run(JobRequest request) {
        PipelineJob job = new PipelineJob(request);

        try {
            job.setStatus(JobStatus.INGESTING);
            IngestResult ingestResult = ingestService.process(request);
            job.setIngestResult(ingestResult);

            job.setStatus(JobStatus.ANALYZING);
            AnalysisResult analysisResult = analysisService.process(
                    new AnalysisContext(request, ingestResult));
            job.setAnalysisResult(analysisResult);

            job.setStatus(JobStatus.PROCESSING);
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

            job.setStatus(JobStatus.COMPLIANCE);
            ComplianceResult complianceResult = complianceService.process(
                    new ComplianceContext(request, visualsResult));
            job.setComplianceResult(complianceResult);

            job.setStatus(JobStatus.PACKAGING);
            PackagingResult packagingResult = packagingService.process(
                    new PackagingContext(request, visualsResult, audioResult, complianceResult));
            job.setPackagingResult(packagingResult);

            job.setStatus(JobStatus.COMPLETED);

        } catch (PipelineException e) {
            job.setStatus(JobStatus.FAILED);
            job.setFailureCause(e);
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setFailureCause(new PipelineException("Unexpected error", "UNKNOWN", e));
        }

        return job;
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
            throw new PipelineException("Parallel processing failed", "PROCESSING", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PipelineException("Parallel processing interrupted", "PROCESSING", e);
        }
    }
}