package app.services.audio;

import app.common.PipelineException;
import app.common.PipelineStage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class AiDubberService implements PipelineStage<Map<String, String>, Map<String, String>> {

    @Override
    public Map<String, String> process(Map<String, String> input) throws PipelineException {
        try {
            String roTranslation = input.get("ro");
            if (roTranslation == null || roTranslation.isBlank()) {
                throw new PipelineException("Missing Romanian translation path", "PROCESSING");
            }

            Path roTranslationPath = Path.of(roTranslation);
            Path jobRoot = roTranslationPath.getParent().getParent();
            Path roDubPath = jobRoot.resolve("audio").resolve("ro_dub_synthetic.aac");

            Files.createDirectories(roDubPath.getParent());
            Files.writeString(roDubPath, "stub synthetic audio");

            return Map.of("ro", roDubPath.toString());
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("AI dubbing failed", "PROCESSING", e);
        }
    }
}
