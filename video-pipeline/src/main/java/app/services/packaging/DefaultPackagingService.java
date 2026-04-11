package app.services.packaging;

import app.common.PipelineException;
import app.common.PipelineStageName;
import app.common.PipelineJson;
import app.model.PackagingContext;
import app.model.PackagingResult;
import app.model.TranscodedVideo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultPackagingService implements PackagingService {

    public DefaultPackagingService() {}

    @Override
    public PackagingResult process(PackagingContext input) throws PipelineException {
        String jobId = input.jobRequest().jobId();
        Path manifestPath = Path.of("output", jobId, "metadata", "manifest.json");

        List<TranscodedVideo> deliveryVideos = input.complianceResult().processedVideos();

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("jobId", jobId);
        manifest.put("sourceFile", input.jobRequest().sourceFile());
        manifest.put("transcriptPath", input.audioResult().transcriptPath());
        manifest.put("translations", input.audioResult().translations());
        manifest.put("syntheticAudio", input.audioResult().syntheticAudio());
        manifest.put("transcodedVideos", deliveryVideos.stream().map(this::videoToMap).toList());
        manifest.put("complianceFlags", input.complianceResult().flags());
        manifest.put("spriteMapPath", input.visualsResult().spriteMapPath());
        manifest.put("sceneAnalysisPath", Path.of("output", jobId, "metadata", "scene_analysis.json").toString().replace('\\', '/'));
        manifest.put("thumbnails", input.visualsResult().thumbnailPaths());

        try {
            PipelineJson.writeDocument(manifestPath, manifest);
        } catch (IOException e) {
            throw new PipelineException("Packaging failed", PipelineStageName.PACKAGING, e);
        }

        List<String> encryptedAssets = deliveryVideos.stream().map(v -> v.path() + ".drm").toList();

        return new PackagingResult(manifestPath.toString(), encryptedAssets);
    }

    private Map<String, String> videoToMap(TranscodedVideo v) {
        return Map.of(
                "path", v.path(),
                "codec", v.codec(),
                "resolution", v.resolution()
        );
    }
}
