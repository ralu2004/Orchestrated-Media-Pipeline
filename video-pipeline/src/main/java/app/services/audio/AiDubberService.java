package app.services.audio;

import app.common.PipelineException;
import app.common.PipelineStageName;
import app.common.PipelineStage;

import java.nio.file.Path;
import java.util.Map;

/**
 * Generates synthetic localized audio from translation files using {@code scripts/dub.py}.
 *
 * Input maps language codes to translation file paths; output maps codes to generated audio paths.
 */
public class AiDubberService implements PipelineStage<Map<String, String>, Map<String, String>> {

    @Override
    public Map<String, String> process(Map<String, String> input) throws PipelineException {
        try {
            String roTranslation = input.get("ro");
            if (roTranslation == null || roTranslation.isBlank()) {
                throw new PipelineException("Missing Romanian translation path", PipelineStageName.PROCESSING);
            }

            Path roTranslationPath = Path.of(roTranslation);
            Path jobRoot = roTranslationPath.getParent().getParent();
            Path roDubPath = jobRoot.resolve("audio").resolve("ro_dub_synthetic.aac");

            runPythonDub(roTranslationPath.toString(), roDubPath.toString(), "ro");

            return Map.of("ro", roDubPath.toString());
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("AI dubbing failed", PipelineStageName.PROCESSING, e);
        }
    }

    private void runPythonDub(String translationPath, String outputAudioPath, String language) throws PipelineException {
        String pythonBin = System.getenv("PYTHON_BIN");
        if (pythonBin == null || pythonBin.isBlank()) {
            pythonBin = "python";
        }

        String scriptPath = Path.of("scripts", "dub.py").toString();
        ProcessBuilder pb = new ProcessBuilder(pythonBin, scriptPath, translationPath, outputAudioPath, language);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new PipelineException("Python dubbing failed: " + output, PipelineStageName.PROCESSING);
            }
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Python dubbing failed", PipelineStageName.PROCESSING, e);
        }
    }
}
