package app.services.audio;

import app.common.PipelineException;
import app.common.PipelineStage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class TranslationService implements PipelineStage<String, Map<String, String>> {


    @Override
    public Map<String, String> process(String input) throws PipelineException {
        try {
            Path transcriptPath = Path.of(input);
            Path jobRoot = transcriptPath.getParent().getParent();
            Path roTranslationPath = jobRoot.resolve("text").resolve("ro_translation.txt");

            Files.createDirectories(roTranslationPath.getParent());
            Files.writeString(roTranslationPath, "stub romanian translation");

            return Map.of("ro", roTranslationPath.toString());
        } catch (Exception e) {
            throw new PipelineException("Translation failed", "PROCESSING", e);
        }
    }
}
