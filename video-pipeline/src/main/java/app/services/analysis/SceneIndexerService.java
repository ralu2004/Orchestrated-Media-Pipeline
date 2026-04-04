package app.services.analysis;

import app.common.PipelineException;
import app.common.PipelineStage;
import app.model.AnalysisResult;
import app.model.SceneSegment;

import java.util.List;

public class SceneIndexerService implements PipelineStage<AnalysisResult, List<SceneSegment>> {
    @Override
    public List<SceneSegment> process(AnalysisResult input) throws PipelineException {
        return List.of(
                new SceneSegment("00:00:42", "00:05:00", "dialogue"),
                new SceneSegment("00:05:00", "00:10:00", "action"),
                new SceneSegment("00:10:00", "01:23:10", "establishing_shot")
        );
    }
}
