package app.services.packaging;

import app.common.PipelineException;
import app.model.PackagingContext;
import app.model.PackagingResult;
import app.model.TranscodedVideo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultPackagingService implements PackagingService {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public DefaultPackagingService() {}

    @Override
    public PackagingResult process(PackagingContext input) throws PipelineException {
        try {
            String jobId = input.jobRequest().jobId();
            Path manifestPath = Path.of("output", jobId, "metadata", "manifest.json");
            Files.createDirectories(manifestPath.getParent());

            Map<String, Object> manifest = new HashMap<>();
            manifest.put("jobId", jobId);
            manifest.put("sourceFile", input.jobRequest().sourceFile());
            manifest.put("transcriptPath", input.audioResult().transcriptPath());
            manifest.put("translations", input.audioResult().translations());
            manifest.put("syntheticAudio", input.audioResult().syntheticAudio());
            manifest.put("transcodedVideos", input.visualsResult().transcodedVideos().stream()
                    .map(this::videoToMap)
                    .toList());
            manifest.put("complianceFlags", input.complianceResult().flags());
            manifest.put("spriteMapPath", input.visualsResult().spriteMapPath());

            MAPPER.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), manifest);

            List<String> encryptedAssets = input.visualsResult().transcodedVideos().stream()
                    .map(v -> v.path() + ".drm")
                    .toList();

            return new PackagingResult(manifestPath.toString(), encryptedAssets);
        } catch (Exception e) {
            throw new PipelineException("Packaging failed", "PACKAGING", e);
        }
    }

    private Map<String, String> videoToMap(TranscodedVideo v) {
        return Map.of(
                "path", v.path(),
                "codec", v.codec(),
                "resolution", v.resolution()
        );
    }
}