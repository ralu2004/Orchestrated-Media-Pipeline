package app;

import app.common.FfmpegRunner;
import app.common.PipelineStageName;
import app.model.JobRequest;
import app.model.JobStatus;
import app.orchestrator.Orchestrator;
import app.orchestrator.PipelineJob;
import app.services.analysis.*;
import app.services.audio.*;
import app.services.compliance.ComplianceService;
import app.services.compliance.DefaultComplianceService;
import app.services.compliance.RegionalBrandingService;
import app.services.compliance.SafetyScannerService;
import app.services.ingest.DefaultIngestService;
import app.services.ingest.FormatValidatorService;
import app.services.ingest.IngestService;
import app.services.ingest.IntegrityCheckService;
import app.services.packaging.DefaultPackagingService;
import app.services.packaging.PackagingService;
import app.services.visuals.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -jar video-pipeline.jar <jobId> <sourceFile> [expectedChecksum]");
            return;
        }

        String jobId = args[0];
        String sourceFile = args[1];
        String expectedChecksum = args.length >= 3 ? args[2] : null;

        JobRequest request = new JobRequest(jobId, sourceFile, expectedChecksum);

        FfmpegRunner ingestRunner = new FfmpegRunner(PipelineStageName.INGESTING);
        FfmpegRunner analysisRunner = new FfmpegRunner(PipelineStageName.ANALYZING);
        FfmpegRunner visualsRunner = new FfmpegRunner(PipelineStageName.VISUALS);
        FfmpegRunner audioFfmpegRunner = new FfmpegRunner(PipelineStageName.PROCESSING);
        FfmpegRunner complianceRunner = new FfmpegRunner(PipelineStageName.COMPLIANCE);

        IngestService ingestService = new DefaultIngestService(
                new IntegrityCheckService(),
                new FormatValidatorService(ingestRunner));

        ExecutorService executor = Executors.newFixedThreadPool(2);

        AnalysisService analysisService = new DefaultAnalysisService(
                executor,
                analysisRunner,
                new IntroOutroDetectorService(),
                new CreditRollerService(),
                new SceneIndexerService());

        VisualsService visualsService = new DefaultVisualsService(
                new SceneComplexityService(visualsRunner),
                new TranscoderService(visualsRunner),
                new SpriteGeneratorService(visualsRunner));

        AudioService audioService = new DefaultAudioService(
                new SpeechToTextService(audioFfmpegRunner),
                new TranslationService(),
                new AiDubberService());
        ComplianceService complianceService = new DefaultComplianceService(
                new SafetyScannerService(complianceRunner),
                new RegionalBrandingService(complianceRunner));
        PackagingService packagingService = new DefaultPackagingService();

        Orchestrator orchestrator = new Orchestrator(
                ingestService,
                analysisService,
                visualsService,
                audioService,
                complianceService,
                packagingService,
                executor);

        try {
            PipelineJob job = orchestrator.run(request);

            System.out.println("Job status: " + job.getStatus());
            if (job.getStatus() == JobStatus.COMPLETED) {
                System.out.println("Manifest: " + job.getPackagingResult().manifestPath());
            } else if (job.getStatus() == JobStatus.FAILED && job.getFailureCause() != null) {
                System.out.println("Failed at stage: " + job.getFailureCause().getStageName());
                System.out.println("Reason: " + job.getFailureCause().getMessage());
            }
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
    }
}