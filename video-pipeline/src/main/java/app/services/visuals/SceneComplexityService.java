package app.services.visuals;

import app.common.FfmpegRunner;
import app.common.PipelineException;
import app.common.PipelineStage;
import app.model.EncodingProfile;
import app.model.VisualsContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Analyzes the source video using ffprobe to derive a dynamic encoding profile.
 *
 * We use Bits Per Pixel (BPP) as a proxy for visual entropy:
 *   BPP = bitrate / (width * height * fps)
 *
 * Trade-off: a more accurate approach would sample frames using ffmpeg's
 * entropy filter, but that requires decoding every frame which is
 * slow for large files.
 */
public class SceneComplexityService implements PipelineStage<VisualsContext, EncodingProfile> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FfmpegRunner runner;

    public SceneComplexityService(FfmpegRunner runner) {
        this.runner = runner;
    }

    @Override
    public EncodingProfile process(VisualsContext input) throws PipelineException {
        try {
            String ffprobeOutput = runFfprobe(input.jobRequest().sourceFile());
            JsonNode videoStream = parseVideoStream(ffprobeOutput);
            double bpp = calculateBpp(videoStream);
            return deriveEncodingProfile(bpp);
        } catch (Exception e) {
            throw new PipelineException("Scene complexity analysis failed", "PROCESSING", e);
        }
    }

    private String runFfprobe(String sourceFile) throws PipelineException {
        return runner.run(List.of(
                "ffprobe", "-v", "quiet",
                "-print_format", "json",
                "-show_streams",
                sourceFile
        ));
    }

    private JsonNode parseVideoStream(String ffprobeOutput) throws Exception {
        JsonNode root = MAPPER.readTree(ffprobeOutput);
        for (JsonNode stream : root.path("streams")) {
            if ("video".equals(stream.path("codec_type").asText())) {
                return stream;
            }
        }

        throw new PipelineException("No video stream found", "PROCESSING");
    }

    private double calculateBpp(JsonNode videoStream) {
        long bitrate = videoStream.path("bit_rate").asLong();
        int width = videoStream.path("width").asInt();
        int height = videoStream.path("height").asInt();
        String fpsString = videoStream.path("r_frame_rate").asText();

        String[] fpsParts = fpsString.split("/");
        double fps = Double.parseDouble(fpsParts[0]) / Double.parseDouble(fpsParts[1]);

        return bitrate / (double) (width * height * fps);
    }

    private EncodingProfile deriveEncodingProfile(double bpp) {
        if (bpp < 0.05) return new EncodingProfile(1_000_000, 28);
        if (bpp < 0.10) return new EncodingProfile(2_000_000, 23);
        return new EncodingProfile(4_000_000, 18);
    }
}