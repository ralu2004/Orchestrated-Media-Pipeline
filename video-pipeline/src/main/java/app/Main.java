package app;

import app.common.FfmpegRunner;
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

        FfmpegRunner ingestRunner = new FfmpegRunner("INGESTING");
        FfmpegRunner analysisRunner = new FfmpegRunner("ANALYZING");
        FfmpegRunner processingRunner = new FfmpegRunner("PROCESSING");
        FfmpegRunner complianceRunner = new FfmpegRunner("COMPLIANCE");

        IngestService ingestService = new DefaultIngestService(
                new IntegrityCheckService(),
                new FormatValidatorService(ingestRunner));

        AnalysisService analysisService = new DefaultAnalysisService(
                analysisRunner,
                new IntroOutroDetectorService(),
                new CreditRollerService(),
                new SceneIndexerService());

        VisualsService visualsService = new DefaultVisualsService(
                new SceneComplexityService(processingRunner),
                new TranscoderService(processingRunner),
                new SpriteGeneratorService(processingRunner));

        AudioService audioService = new DefaultAudioService(
                new SpeechToTextService(processingRunner),
                new TranslationService(),
                new AiDubberService());
        ComplianceService complianceService = new DefaultComplianceService(
                new SafetyScannerService(complianceRunner),
                new RegionalBrandingService(complianceRunner));
        PackagingService packagingService = new DefaultPackagingService();

        ExecutorService executor = Executors.newFixedThreadPool(2);

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
        }
    }
}