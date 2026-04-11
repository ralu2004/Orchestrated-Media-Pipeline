package app.services.packaging;

import app.common.PipelineException;
import app.common.PipelineStageName;
import app.common.PipelineJson;
import app.model.PackagingContext;
import app.model.PackagingResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DefaultPackagingService implements PackagingService {

    private final DrmWrapper drmWrapper;
    private final ManifestBuilder manifestBuilder;

    public DefaultPackagingService(DrmWrapper drmWrapper, ManifestBuilder manifestBuilder) {
        this.drmWrapper = drmWrapper;
        this.manifestBuilder = manifestBuilder;
    }

    @Override
    public PackagingResult process(PackagingContext input) throws PipelineException {
        String jobId = input.jobRequest().jobId();
        Path manifestPath = Path.of("output", jobId, "manifest.json");

        List<String> drmPaths;
        try {
            drmPaths = drmWrapper.wrapDeliveryAssets(jobId, input.complianceResult().processedVideos());
        } catch (IOException e) {
            throw new PipelineException("DRM wrap failed", PipelineStageName.PACKAGING, e);
        }

        Map<String, Object> manifest = manifestBuilder.build(input, drmPaths);

        try {
            PipelineJson.writeDocument(manifestPath, manifest);
        } catch (IOException e) {
            throw new PipelineException("Packaging failed", PipelineStageName.PACKAGING, e);
        }

        return new PackagingResult(manifestPath.toString().replace('\\', '/'), drmPaths);
    }
}
