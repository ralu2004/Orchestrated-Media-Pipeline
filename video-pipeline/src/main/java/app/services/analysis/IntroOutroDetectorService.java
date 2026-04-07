package app.services.analysis;

import app.common.PipelineException;
import app.common.SubprocessStage;
import app.common.TimestampUtils;
import app.model.AnalysisContext;
import app.model.IntroOutroTimestamps;

import java.util.List;

public class IntroOutroDetectorService extends SubprocessStage<AnalysisContext, IntroOutroTimestamps> {

    @Override
    public IntroOutroTimestamps process(AnalysisContext input) throws PipelineException {
        try {
            String silenceDetect = runSilenceDetect(input.jobRequest().sourceFile());
            String sceneDetect = runSceneDetect(input.jobRequest().sourceFile());
            String introEnd = parseIntroEnd(silenceDetect, sceneDetect, input.ingestResult().formatInfo().duration());
            String outroStart = parseOutroStart(sceneDetect, input.ingestResult().formatInfo().duration());
            return new IntroOutroTimestamps(introEnd, outroStart);
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Intro/Outro detection failed", "ANALYZING", e);
        }
    }

    @Override
    protected String getStageName() {
        return "ANALYZING";
    }

    private String runSilenceDetect(String sourceFile) throws Exception {
        return runProcess(List.of(
                "ffmpeg", "-i", sourceFile,
                "-af", "silencedetect=noise=-30dB:d=0.5",
                "-f", "null", "-"
        ), true);
    }

    private String runSceneDetect(String sourceFile) throws Exception {
        return runProcess(List.of(
                "ffmpeg", "-i", sourceFile,
                "-vf", "select='gt(scene,0.4)',showinfo",
                "-f", "null", "-"
        ), true);
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