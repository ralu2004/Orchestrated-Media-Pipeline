package app.services.visuals;

import app.common.PipelineException;
import app.model.*;

import java.util.List;

/**
 * Orchestrates scene complexity, transcoding, and sprite generation.
 *
 * Each substage writes its own outputs; this class threads {@link VisualsContext} through the chain.
 */
public class DefaultVisualsService implements VisualsService{

    private final SceneComplexityService sceneComplexityService;
    private final TranscoderService transcoderService;
    private final SpriteGeneratorService spriteGeneratorService;

    public DefaultVisualsService(SceneComplexityService sceneComplexityService, TranscoderService transcoderService, SpriteGeneratorService spriteGeneratorService) {
        this.sceneComplexityService = sceneComplexityService;
        this.transcoderService = transcoderService;
        this.spriteGeneratorService = spriteGeneratorService;
    }


    @Override
    public VisualsResult process(JobRequest input) throws PipelineException {
        VisualsContext initial = new VisualsContext(input, null, null);
        EncodingProfile encodingProfile = sceneComplexityService.process(initial);

        VisualsContext withProfile = new VisualsContext(input, encodingProfile, null);
        List<TranscodedVideo> transcodedVideos = transcoderService.process(withProfile);

        VisualsContext withVideos = new VisualsContext(input, encodingProfile, transcodedVideos);
        String spriteMapPath = spriteGeneratorService.process(withVideos);

        return new VisualsResult(encodingProfile, transcodedVideos, spriteMapPath);
    }
}
