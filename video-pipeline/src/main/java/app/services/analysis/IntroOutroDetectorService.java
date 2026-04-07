package app.services.analysis;

import app.common.PipelineException;
import app.common.PipelineStage;
import app.common.TimestampUtils;
import app.model.IntroOutroTimestamps;
import app.model.RawAnalysisData;

public class IntroOutroDetectorService implements PipelineStage<RawAnalysisData, IntroOutroTimestamps> {

    @Override
    public IntroOutroTimestamps process(RawAnalysisData input) throws PipelineException {
        try {
            String introEnd = parseIntroEnd(input.silenceOutput(), input.sceneOutput(), input.durationSeconds());
            String outroStart = parseOutroStart(input.sceneOutput(), input.durationSeconds());
            return new IntroOutroTimestamps(introEnd, outroStart);
        } catch (Exception e) {
            throw new PipelineException("Intro/Outro detection failed", "ANALYZING", e);
        }
    }

    private String parseIntroEnd(String silenceOutput, String sceneOutput, double duration) {
        for (String line : silenceOutput.split("\n")) {
            if (line.contains("silence_end:")) {
                double silenceEnd = parseDoubleAfter(line, "silence_end:");
                if (silenceEnd > 10.0 && silenceEnd < duration * 0.25) {
                    return TimestampUtils.formatTimestamp(silenceEnd);
                }
            }
        }
        return TimestampUtils.formatTimestamp(duration * 0.05);
    }

    private String parseOutroStart(String sceneOutput, double duration) {
        double outroThreshold = duration * 0.85;
        String lastSceneChange = null;

        for (String line : sceneOutput.split("\n")) {
            if (line.contains("pts_time:")) {
                double pts = parseDoubleAfter(line, "pts_time:");
                if (pts >= outroThreshold) {
                    lastSceneChange = TimestampUtils.formatTimestamp(pts);
                }
            }
        }
        return lastSceneChange != null ? lastSceneChange
                : TimestampUtils.formatTimestamp(duration * 0.90);
    }

    private double parseDoubleAfter(String line, String marker) {
        String after = line.split(marker)[1].trim().split("[\\s|]")[0];
        return Double.parseDouble(after);
    }
}