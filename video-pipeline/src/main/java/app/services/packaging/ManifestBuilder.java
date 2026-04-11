package app.services.packaging;

import app.model.PackagingContext;
import app.model.TranscodedVideo;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Assembles the job manifest document (JSON). */
public class ManifestBuilder {

    public Map<String, Object> build(PackagingContext input, List<String> drmAssets) {
        String jobId = input.jobRequest().jobId();
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
        manifest.put("sceneAnalysisPath",
                Path.of("output", jobId, "metadata", "scene_analysis.json").toString().replace('\\', '/'));
        manifest.put("thumbnails", input.visualsResult().thumbnailPaths());
        manifest.put("drmAssets", drmAssets);
        return manifest;
    }

    private Map<String, String> videoToMap(TranscodedVideo v) {
        return Map.of(
                "path", v.path(),
                "codec", v.codec(),
                "resolution", v.resolution());
    }
}
