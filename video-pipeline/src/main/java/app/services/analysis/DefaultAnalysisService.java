package app.services.analysis;

import app.common.FfmpegRunner;
import app.common.PipelineException;
import app.model.*;

import java.util.List;
import java.util.concurrent.*;

/**
 * Coordinates the analysis phase by:
 * 1. Pre-computing all ffmpeg filter outputs once (silencedetect, scenedetect, blackdetect)
 * 2. Running IntroOutroDetectorService and CreditRollerService in parallel (pure parsers)
 * 3. Running SceneIndexerService sequentially after both finish (depends on their results)
 *
 * Pre-computation avoids redundant ffmpeg calls across substages.
 * Parallel parsing reduces wall-clock time since parsing is CPU-light.
 */
public class DefaultAnalysisService implements AnalysisService {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    private final FfmpegRunner runner;
    private final IntroOutroDetectorService introOutroDetector;
    private final CreditRollerService creditRoller;
    private final SceneIndexerService sceneIndexer;

    public DefaultAnalysisService(FfmpegRunner runner,
                                  IntroOutroDetectorService introOutroDetector,
                                  CreditRollerService creditRoller,
                                  SceneIndexerService sceneIndexer) {
        this.runner = runner;
        this.introOutroDetector = introOutroDetector;
        this.creditRoller = creditRoller;
        this.sceneIndexer = sceneIndexer;
    }

    @Override
    public AnalysisResult process(AnalysisContext input) throws PipelineException {
        try {
            RawAnalysisData raw = buildRawAnalysisData(input);

            CompletableFuture<IntroOutroTimestamps> introOutroFuture = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return introOutroDetector.process(raw);
                        } catch (PipelineException e) {
                            throw new RuntimeException(e);
                        }
                    }, EXECUTOR);

            CompletableFuture<String> creditsFuture = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return creditRoller.process(raw);
                        } catch (PipelineException e) {
                            throw new RuntimeException(e);
                        }
                    }, EXECUTOR);

            CompletableFuture.allOf(introOutroFuture, creditsFuture).join();

            IntroOutroTimestamps introOutro = introOutroFuture.get();
            String creditsTimestamp = creditsFuture.get();

            List<SceneSegment> segments = sceneIndexer.process(raw);

            return new AnalysisResult(introOutro, creditsTimestamp, segments);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof PipelineException pe) throw pe;
            throw new PipelineException("Analysis phase failed", "ANALYZING", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PipelineException("Analysis phase interrupted", "ANALYZING", e);
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Analysis phase failed", "ANALYZING", e);
        }
    }

    private RawAnalysisData buildRawAnalysisData(AnalysisContext context) throws PipelineException {
        String sourceFile = context.jobRequest().sourceFile();
        double duration = context.ingestResult().formatInfo().duration();

        String silenceOutput = runner.runCaptureStderr(List.of(
                "ffmpeg", "-i", sourceFile,
                "-af", "silencedetect=noise=-30dB:d=0.5",
                "-f", "null", "-"
        ));

        String sceneOutput = runner.runCaptureStderr(List.of(
                "ffmpeg", "-i", sourceFile,
                "-vf", "select='gt(scene,0.4)',showinfo",
                "-f", "null", "-"
        ));

        String blackdetectOutput = runner.runCaptureStderr(List.of(
                "ffmpeg", "-i", sourceFile,
                "-vf", "blackdetect=d=0.1:pix_th=0.10",
                "-f", "null", "-"
        ));

        return new RawAnalysisData(duration, silenceOutput, sceneOutput, blackdetectOutput);
    }
}