package app.services.ingest;

import app.common.PipelineException;
import app.common.SubprocessStage;
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
public class FormatValidatorService extends SubprocessStage<JobRequest, FormatInfo> {

    private static final Set<String> ACCEPTED_FORMATS = Set.of(
            "mov,mp4,m4a,3gp,3g2,mj2",   // mp4/mov
            "matroska,webm"              // mkv/webm
    );

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
            throw new PipelineException("Format validation failed", "INGESTING", e);
        }
    }

    private String runFfprobe(String sourceFile) throws Exception {
        return runProcess(List.of(
            "ffprobe", "-v", "quiet",
            "-print_format", "json",
            "-show_format",
            sourceFile));
    }

    private FormatInfo parseFormatInfo(String ffprobeOutput) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(ffprobeOutput);
        JsonNode format = root.path("format");
        String formatName = format.path("format_name").asText();
        double duration = format.path("duration").asDouble();
        return new FormatInfo(formatName, duration);
    }

    private void validateFormat(String format) throws PipelineException {
        if (!ACCEPTED_FORMATS.contains(format)) {
            throw new PipelineException(
                    "Unsupported format: " + format + ". Accepted: " + ACCEPTED_FORMATS,
                    "INGESTING");
        }
    }

    @Override
    protected String getStageName() {
        return "INGESTING";
    }
}