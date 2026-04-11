package app.services.ingest;

import app.common.FfmpegRunner;
import app.common.PipelineException;
import app.common.PipelineStage;
import app.common.PipelineStageName;
import app.model.FormatInfo;
import app.model.JobRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

/**
 * Validates the source file format using ffprobe.
 *
 * Extracts the format name from the container and checks it against
 * a set of accepted studio formats.
 */
public class FormatValidatorService implements PipelineStage<JobRequest, FormatInfo> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> ACCEPTED_FORMATS = Set.of(
            "mov,mp4,m4a,3gp,3g2,mj2",   // mp4/mov
            "matroska,webm"              // mkv/webm
    );

    private final FfmpegRunner runner;

    public FormatValidatorService(FfmpegRunner runner) {
        this.runner = runner;
    }

    @Override
    public FormatInfo process(JobRequest input) throws PipelineException {
        try {
            String ffprobeOutput = runFfprobe(input.sourceFile());
            FormatInfo format = parseFormatInfo(ffprobeOutput);
            validateFormat(format.fileFormat());
            return format;
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Format validation failed", PipelineStageName.INGESTING, e);
        }
    }

    private String runFfprobe(String sourceFile) throws PipelineException {
        return runner.run(List.of(
            "ffprobe", "-v", "quiet",
            "-print_format", "json",
            "-show_format",
            sourceFile));
    }

    private FormatInfo parseFormatInfo(String ffprobeOutput) throws Exception {
        JsonNode root = MAPPER.readTree(ffprobeOutput);
        JsonNode format = root.path("format");
        String formatName = format.path("format_name").asText();
        double duration = format.path("duration").asDouble();
        return new FormatInfo(formatName, duration);
    }

    private void validateFormat(String format) throws PipelineException {
        if (!ACCEPTED_FORMATS.contains(format)) {
            throw new PipelineException("Unsupported format: " + format + ". Accepted: " + ACCEPTED_FORMATS, PipelineStageName.INGESTING);
        }
    }
}