package app.services.visuals;

import app.common.PipelineException;
import app.common.PipelineStage;
import app.model.VisualsContext;

public class SpriteGeneratorService implements PipelineStage<VisualsContext, String> {
    @Override
    public String process(VisualsContext input) throws PipelineException {
        return "";
    }
}
