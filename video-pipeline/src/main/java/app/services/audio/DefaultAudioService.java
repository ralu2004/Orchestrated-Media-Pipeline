package app.services.audio;

import app.common.PipelineException;
import app.common.PipelineStageName;
import app.model.AudioContext;
import app.model.AudioResult;

import java.util.Map;

/**
 * Orchestrates the audio pipeline: speech-to-text, translation, AI dubbing.
 *
 * Delegates file generation to substages; aggregates paths into {@link AudioResult}.
 */
public class DefaultAudioService implements AudioService {

    private final SpeechToTextService speechToTextService;
    private final TranslationService translationService;
    private final AiDubberService aiDubberService;

    public DefaultAudioService(SpeechToTextService speechToTextService,
                               TranslationService translationService,
                               AiDubberService aiDubberService) {
        this.speechToTextService = speechToTextService;
        this.translationService = translationService;
        this.aiDubberService = aiDubberService;
    }

    @Override
    public AudioResult process(AudioContext input) throws PipelineException {
        try {
            String transcriptPath = speechToTextService.process(input);
            Map<String, String> translations = translationService.process(transcriptPath);
            Map<String, String> syntheticAudio = aiDubberService.process(translations);

            return new AudioResult(transcriptPath, translations, syntheticAudio);
        } catch (Exception e) {
            throw new PipelineException("Audio processing failed", PipelineStageName.AUDIO, e);
        }
    }
}
