package app.services.analysis;

import app.common.FfmpegRunner;
import app.common.PipelineException;
import app.common.PipelineStageName;
import app.common.PipelineJson;
import app.model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Coordinates the analysis phase:
 * 1. Pre-computes all ffmpeg filter outputs once (silencedetect, scenedetect, blackdetect)
 * 2. Runs IntroOutroDetectorService and CreditRollerService in parallel (pure parsers)
 * 3. Runs SceneIndexerService sequentially after both finish (depends on their results)
 * 4. Writes {@code output/{jobId}/metadata/scene_analysis.json}
 */
public class DefaultAnalysisService implements AnalysisService {

    private final ExecutorService parallel;
    private final FfmpegRunner runner;
    private final IntroOutroDetectorService introOutroDetector;
    private final CreditRollerService creditRoller;
    private final SceneIndexerService sceneIndexer;

    public DefaultAnalysisService(ExecutorService parallel,
                                  FfmpegRunner runner,
                                  IntroOutroDetectorService introOutroDetector,
                                  CreditRollerService creditRoller,
                                  SceneIndexerService sceneIndexer) {
        this.parallel = parallel;
        this.runner = runner;
        this.introOutroDetector = introOutroDetector;
        this.creditRoller = creditRoller;
        this.sceneIndexer = sceneIndexer;
    }

    @Override
    public AnalysisResult process(AnalysisContext input) throws PipelineException {
        try {
            RawAnalysisData raw = buildRawAnalysisData(input);

            CompletableFuture<IntroOutroDetectorService.IntroOutroTimestamps> introOutroFuture = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return introOutroDetector.process(raw);
                        } catch (PipelineException e) {
                            throw new RuntimeException(e);
                        }
                    }, parallel);

            CompletableFuture<String> creditsFuture = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return creditRoller.process(raw);
                        } catch (PipelineException e) {
                            throw new RuntimeException(e);
                        }
                    }, parallel);

            CompletableFuture.allOf(introOutroFuture, creditsFuture).join();

            IntroOutroDetectorService.IntroOutroTimestamps introOutro = introOutroFuture.get();
            String creditsTimestamp = creditsFuture.get();

            List<SceneSegment> segments = sceneIndexer.process(raw);

            AnalysisResult analysisResult = new AnalysisResult(
                    introOutro.introEnd(),
                    introOutro.outroStart(),
                    creditsTimestamp,
                    segments);

            writeSceneAnalysisJson(input.jobRequest().jobId(), analysisResult);

            return analysisResult;

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re && re.getCause() instanceof PipelineException pe) {
                throw pe;
            }
            throw new PipelineException("Analysis phase failed", PipelineStageName.ANALYZING, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PipelineException("Analysis phase interrupted", PipelineStageName.ANALYZING, e);
        } catch (PipelineException e) {
            throw e;
        }
    }

    private void writeSceneAnalysisJson(String jobId, AnalysisResult result) throws PipelineException {
        Path path = Path.of("output", jobId, "metadata", "scene_analysis.json");

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("schemaVersion", 1);
        doc.put("jobId", jobId);
        doc.put("source", "ffmpeg/ffprobe scene classifiers");
        doc.put("introEnd", result.introEnd());
        doc.put("outroStart", result.outroStart());
        doc.put("creditsTimestamp", result.creditsTimestamp());
        doc.put("segments", segmentsToMaps(result.segments()));

        try {
            PipelineJson.writeDocument(path, doc);
        } catch (IOException e) {
            throw new PipelineException("Failed to write scene_analysis.json", PipelineStageName.ANALYZING, e);
        }
    }

    private static List<Map<String, String>> segmentsToMaps(List<SceneSegment> segments) {
        return segments.stream()
                .map(s -> Map.of(
                        "startTime", s.startTime(),
                        "endTime", s.endTime(),
                        "category", s.category()))
                .toList();
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
