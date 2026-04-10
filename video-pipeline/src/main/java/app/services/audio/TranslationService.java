package app.services.audio;

import app.common.PipelineException;
import app.common.PipelineStage;

import java.nio.file.Path;
import java.util.Map;

public class TranslationService implements PipelineStage<String, Map<String, String>> {


    @Override
    public Map<String, String> process(String input) throws PipelineException {
        try {
            Path transcriptPath = Path.of(input);
            Path jobRoot = transcriptPath.getParent().getParent();
            Path roTranslationPath = jobRoot.resolve("text").resolve("ro_translation.txt");

            runPythonTranslate(transcriptPath.toString(), roTranslationPath.toString());

            return Map.of("ro", roTranslationPath.toString());
        } catch (Exception e) {
            throw new PipelineException("Translation failed", "PROCESSING", e);
        }
    }

    private void runPythonTranslate(String transcriptPath, String outputPath) throws PipelineException {
        String pythonBin = System.getenv("PYTHON_BIN");
        if (pythonBin == null || pythonBin.isBlank()) {
            pythonBin = "python";
        }

        String scriptPath = Path.of("scripts", "translate.py").toString();
        ProcessBuilder pb = new ProcessBuilder(
                pythonBin,
                scriptPath,
                transcriptPath,
                outputPath,
                "ro"
        );
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new PipelineException("Python translation failed: " + output, "PROCESSING");
            }
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Python translation failed", "PROCESSING", e);
        }
    }
}
