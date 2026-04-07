package app.services.analysis;

import app.common.PipelineException;
import app.common.SubprocessStage;
import app.common.TimestampUtils;
import app.model.AnalysisContext;

import java.util.List;

public class CreditRollerService extends SubprocessStage<AnalysisContext, String> {

    @Override
    public String process(AnalysisContext input) throws PipelineException {
        try {
            String ffmpegOutput = runFfmpeg(input.jobRequest().sourceFile());
            double duration = input.ingestResult().formatInfo().duration();
            return parseLastBlackSegment(ffmpegOutput, duration);
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Credit detection failed", "ANALYZING", e);
        }
    }

    @Override
    protected String getStageName() {
        return "ANALYZING";
    }

    private String runFfmpeg(String sourceFile) throws Exception {
        return runProcess(List.of(
            "ffmpeg", "-i", sourceFile,
            "-vf", "blackdetect=d=0.1:pix_th=0.10",
            "-f", "null", "-"
        ), true);
    }

    private String parseLastBlackSegment(String ffmpegOutput, double duration) throws PipelineException {
        double creditsThreshold = duration * 0.80; // last 20% of video
        String lastTimestamp = null;

        for (String line : ffmpegOutput.split("\n")) {
            if (line.contains("black_start:")) {
                String[] parts = line.split("black_start:")[1].split(" ");
                double blackStart = Double.parseDouble(parts[0].trim());

                if (blackStart >= creditsThreshold) {
                    if (lastTimestamp == null) {
                        lastTimestamp = TimestampUtils.formatTimestamp(blackStart);
                    }
                }
            }
        }

        if (lastTimestamp == null) {
            // fall back to 90% of duration
            return TimestampUtils.formatTimestamp(duration * 0.90);
        }

        return lastTimestamp;
    }
}
