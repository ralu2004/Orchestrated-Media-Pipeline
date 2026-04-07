package app.services.analysis;

import app.common.PipelineException;
import app.common.PipelineStage;
import app.common.TimestampUtils;

public class CreditRollerService implements PipelineStage<RawAnalysisData, String> {

    @Override
    public String process(RawAnalysisData input) throws PipelineException {
        try {
            return parseLastBlackSegment(input.blackdetectOutput(), input.durationSeconds());
        } catch (Exception e) {
            throw new PipelineException("Credit detection failed", "ANALYZING", e);
        }
    }

    private String parseLastBlackSegment(String ffmpegOutput, double duration) throws PipelineException {
        double creditsThreshold = duration * 0.80; // last 20% of video
        String lastTimestamp = null;

        for (String line : ffmpegOutput.split("\n")) {
            if (line.contains("black_start:")) {
                String[] parts = line.split("black_start:")[1].split(" ");
                double blackStart = Double.parseDouble(parts[0].trim());

                if (blackStart >= creditsThreshold) {
                    lastTimestamp = TimestampUtils.formatTimestamp(blackStart);
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
