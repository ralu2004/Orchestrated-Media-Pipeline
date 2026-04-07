package app.services.audio;

import app.common.PipelineStage;
import app.model.AudioContext;
import app.model.AudioResult;

public interface AudioService extends PipelineStage<AudioContext, AudioResult> {
}
