package app.services.analysis;

import app.common.PipelineException;
import app.common.SubprocessStage;
import app.common.TimestampUtils;
import app.model.AnalysisContext;
import app.model.SceneSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects scene boundaries using ffmpeg's scene filter and classifies
 * each segment based on visual complexity and speech presence.
 *
 * Classification logic:
 *   short gap + no silence  → action
 *   long gap  + silence     → dialogue
 *   long gap  + no silence  → establishing_shot
 *
 * Skips segments falling within intro, outro, or credits.
 */
public class SceneIndexerService extends SubprocessStage<AnalysisContext, List<SceneSegment>> {

    private static final double SHORT_GAP_THRESHOLD = 3.0; // seconds

    @Override
    public List<SceneSegment> process(AnalysisContext input) throws PipelineException {
        try {
            String sourceFile = input.jobRequest().sourceFile();
            String sceneOutput = runSceneDetect(sourceFile);
            String silenceOutput = runSilenceDetect(sourceFile);
            return classifySegments(sceneOutput, silenceOutput, input);
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Scene indexing failed", "ANALYZING", e);
        }
    }

    @Override
    protected String getStageName() {
        return "ANALYZING";
    }

    private String runSceneDetect(String sourceFile) throws Exception {
        return runProcess(List.of(
                "ffmpeg", "-i", sourceFile,
                "-vf", "select='gt(scene,0.4)',showinfo",
                "-f", "null", "-"
        ), true);
    }

    private String runSilenceDetect(String sourceFile) throws Exception {
        return runProcess(List.of(
                "ffmpeg", "-i", sourceFile,
                "-af", "silencedetect=noise=-30dB:d=0.5",
                "-f", "null", "-"
        ), true);
    }

    private List<SceneSegment> classifySegments(String sceneOutput, String silenceOutput, AnalysisContext input) {
        List<Double> sceneTimes = parseSceneTimestamps(sceneOutput);
        List<double[]> silenceRanges = parseSilenceRanges(silenceOutput);

        double duration = input.ingestResult().formatInfo().duration();
        double introEnd = parseSeconds(input.analysisResult().introOutro().introEnd());
        double outroStart = parseSeconds(input.analysisResult().introOutro().outroStart());
        double creditsStart = parseSeconds(input.analysisResult().creditsTimestamp());

        sceneTimes.add(0, 0.0);
        sceneTimes.add(duration);

        List<SceneSegment> segments = new ArrayList<>();

        for (int i = 0; i < sceneTimes.size() - 1; i++) {
            double start = sceneTimes.get(i);
            double end = sceneTimes.get(i + 1);

            if (end <= introEnd) continue;
            if (start >= outroStart) continue;
            if (start >= creditsStart) continue;

            String category = classifySegment(start, end, silenceRanges);
            segments.add(new SceneSegment(
                    TimestampUtils.formatTimestamp(start),
                    TimestampUtils.formatTimestamp(end),
                    category));
        }

        return segments;
    }

    private List<Double> parseSceneTimestamps(String sceneOutput) {
        List<Double> timestamps = new ArrayList<>();
        for (String line : sceneOutput.split("\n")) {
            if (line.contains("pts_time:")) {
                try {
                    String after = line.split("pts_time:")[1].trim().split("[\\s|]")[0];
                    timestamps.add(Double.parseDouble(after));
                } catch (Exception ignored) {}
            }
        }
        return timestamps;
    }

    private List<double[]> parseSilenceRanges(String silenceOutput) {
        List<double[]> ranges = new ArrayList<>();
        double silenceStart = -1;

        for (String line : silenceOutput.split("\n")) {
            if (line.contains("silence_start:")) {
                silenceStart = parseDoubleAfter(line, "silence_start:");
            } else if (line.contains("silence_end:") && silenceStart >= 0) {
                double silenceEnd = parseDoubleAfter(line, "silence_end:");
                ranges.add(new double[]{silenceStart, silenceEnd});
                silenceStart = -1;
            }
        }
        return ranges;
    }

    private String classifySegment(double start, double end, List<double[]> silenceRanges) {
        double gap = end - start;
        boolean hasSpeech = !isSilent(start, end, silenceRanges);

        if (gap < SHORT_GAP_THRESHOLD && hasSpeech) return "action";
        if (gap >= SHORT_GAP_THRESHOLD && hasSpeech) return "dialogue";
        return "establishing_shot";
    }

    private boolean isSilent(double start, double end, List<double[]> silenceRanges) {
        for (double[] range : silenceRanges) {
            // silence range overlaps with segment
            if (range[0] < end && range[1] > start) return true;
        }
        return false;
    }

    private double parseDoubleAfter(String line, String marker) {
        String after = line.split(marker)[1].trim().split("[\\s|]")[0];
        return Double.parseDouble(after);
    }

    private double parseSeconds(String timestamp) {
        String[] parts = timestamp.split(":");
        return Integer.parseInt(parts[0]) * 3600
                + Integer.parseInt(parts[1]) * 60
                + Integer.parseInt(parts[2]);
    }
}