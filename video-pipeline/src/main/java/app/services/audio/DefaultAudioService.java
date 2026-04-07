package app.services.audio;

import app.common.PipelineException;
import app.model.AudioContext;
import app.model.AudioResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class DefaultAudioService implements AudioService {

    public DefaultAudioService() {
    }

    @Override
    public AudioResult process(AudioContext input) throws PipelineException {
        try {
            String jobId = input.jobRequest().jobId();

            Path transcriptPath = Path.of("output", jobId, "text", "source_transcript.txt");
            Path roTranslationPath = Path.of("output", jobId, "text", "ro_translation.txt");
            Path roDubPath = Path.of("output", jobId, "audio", "ro_dub_synthetic.aac");

            Files.createDirectories(transcriptPath.getParent());
            Files.createDirectories(roDubPath.getParent());

            Files.writeString(transcriptPath, "stub transcript");
            Files.writeString(roTranslationPath, "stub romanian translation");
            Files.writeString(roDubPath, "stub synthetic audio");

            return new AudioResult(
                    transcriptPath.toString(),
                    Map.of("ro", roTranslationPath.toString()),
                    Map.of("ro", roDubPath.toString()));
        } catch (Exception e) {
            throw new PipelineException("Audio processing failed", "AUDIO", e);
        }
    }
}
