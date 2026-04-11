package app;

import app.common.FfmpegRunner;
import app.common.PipelineStageName;
import app.orchestrator.Orchestrator;
import app.services.analysis.*;
import app.services.audio.*;
import app.services.compliance.DefaultComplianceService;
import app.services.compliance.RegionalBrandingService;
import app.services.compliance.SafetyScannerService;
import app.services.ingest.DefaultIngestService;
import app.services.ingest.FormatValidatorService;
import app.services.ingest.IntegrityCheckService;
import app.services.packaging.DefaultPackagingService;
import app.services.visuals.*;

import java.util.concurrent.ExecutorService;

/**
 * Composition root: wires default service implementations into an {@link Orchestrator}.
 */
public final class PipelineFactory {

    private PipelineFactory() {}

    public static Orchestrator createOrchestrator(ExecutorService executor) {
        FfmpegRunner ingestRunner = new FfmpegRunner(PipelineStageName.INGESTING);
        FfmpegRunner analysisRunner = new FfmpegRunner(PipelineStageName.ANALYZING);
        FfmpegRunner visualsRunner = new FfmpegRunner(PipelineStageName.VISUALS);
        FfmpegRunner audioFfmpegRunner = new FfmpegRunner(PipelineStageName.PROCESSING);
        FfmpegRunner complianceRunner = new FfmpegRunner(PipelineStageName.COMPLIANCE);

        return new Orchestrator(
                new DefaultIngestService(
                        new IntegrityCheckService(),
                        new FormatValidatorService(ingestRunner)),
                new DefaultAnalysisService(
                        executor,
                        analysisRunner,
                        new IntroOutroDetectorService(),
                        new CreditRollerService(),
                        new SceneIndexerService()),
                new DefaultVisualsService(
                        new SceneComplexityService(visualsRunner),
                        new TranscoderService(visualsRunner),
                        new SpriteGeneratorService(visualsRunner)),
                new DefaultAudioService(
                        new SpeechToTextService(audioFfmpegRunner),
                        new TranslationService(),
                        new AiDubberService()),
                new DefaultComplianceService(
                        new SafetyScannerService(complianceRunner),
                        new RegionalBrandingService(complianceRunner)),
                new DefaultPackagingService(),
                executor);
    }
}
