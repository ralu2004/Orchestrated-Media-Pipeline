package app.services.analysis;

import app.common.PipelineException;
import app.common.PipelineStage;
import app.common.TimestampUtils;
import app.model.SceneSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses scene boundaries and classifies segments based on visual complexity
 * and speech presence using precomputed analysis logs.
 *
 * Classification logic:
 *   short gap + no silence  → action
 *   long gap  + silence     → dialogue
 *   long gap  + no silence  → establishing_shot
 */
public class SceneIndexerService implements PipelineStage<RawAnalysisData, List<SceneSegment>> {

    private static final double SHORT_GAP_THRESHOLD = 3.0; // seconds

    @Override
    public List<SceneSegment> process(RawAnalysisData input) throws PipelineException {
        try {
            return classifySegments(input.sceneOutput(), input.silenceOutput(), input.durationSeconds());
        } catch (Exception e) {
            throw new PipelineException("Scene indexing failed", "ANALYZING", e);
        }
    }

    private List<SceneSegment> classifySegments(String sceneOutput, String silenceOutput, double duration) {
        List<Double> sceneTimes = parseSceneTimestamps(sceneOutput);
        List<double[]> silenceRanges = parseSilenceRanges(silenceOutput);

        sceneTimes.add(0, 0.0);
        sceneTimes.add(duration);

        List<SceneSegment> segments = new ArrayList<>();

        for (int i = 0; i < sceneTimes.size() - 1; i++) {
            double start = sceneTimes.get(i);
            double end = sceneTimes.get(i + 1);

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
}