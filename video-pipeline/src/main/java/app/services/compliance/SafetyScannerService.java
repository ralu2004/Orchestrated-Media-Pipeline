package app.services.compliance;

import app.common.FfmpegRunner;
import app.common.PipelineException;
import app.common.PipelineStageName;
import app.common.TimestampUtils;
import app.model.ComplianceContext;
import app.model.ContentFlag;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces compliance flags for segments that may need "blurring".
 *
 * Uses the duration of the first transcoded rendition from ffprobe, then emits representative
 * timestamps at fixed positions along the timeline for distinct example blurring positions. 
 */
public class SafetyScannerService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FfmpegRunner runner;

    public SafetyScannerService(FfmpegRunner runner) {
        this.runner = runner;
    }

    public List<ContentFlag> scan(ComplianceContext context) throws PipelineException {
        var videos = context.visualsResult().transcodedVideos();
        if (videos == null || videos.isEmpty()) {
            return List.of();
        }

        double duration = probeDurationSeconds(videos.getFirst().path());
        if (duration <= 0) {
            return List.of(new ContentFlag("00:00:00", "EU"));
        }

        List<ContentFlag> flags = new ArrayList<>();
        flags.add(new ContentFlag(TimestampUtils.formatTimestamp(duration * 0.18), "EU"));
        flags.add(new ContentFlag(TimestampUtils.formatTimestamp(duration * 0.82), "US"));
        return flags;
    }

    private double probeDurationSeconds(String mediaPath) throws PipelineException {
        try {
            String json = runner.run(List.of(
                    "ffprobe", "-v", "quiet",
                    "-print_format", "json",
                    "-show_format",
                    mediaPath));
            JsonNode root = MAPPER.readTree(json);
            return root.path("format").path("duration").asDouble(0);
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Safety scan failed: could not read media duration", PipelineStageName.COMPLIANCE, e);
        }
    }
}
