package app.services.audio;

import app.common.PipelineStage;
import app.model.AudioContext;
import app.model.AudioResult;

/**
 * Audio phase: transcribe, translate, and generate synthetic audio tracks.
 */
public interface AudioService extends PipelineStage<AudioContext, AudioResult> {
}
