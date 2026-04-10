package app.services.audio;

import app.common.PipelineException;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TranslationServiceTest {

    @Test
    void translates_transcript_and_writes_output_file() throws Exception {
        String pythonBin = System.getenv("PYTHON_BIN");
        if (pythonBin == null || pythonBin.isBlank()) {
            pythonBin = "python";
        }
        assumeTrue(canRunPython(pythonBin), "Python is not available: " + pythonBin);

        Path moduleDir = Path.of("").toAbsolutePath();
        Path script = moduleDir.resolve("scripts/translate.py").normalize().toAbsolutePath();
        assumeTrue(Files.exists(script), "translate.py not found at " + script);

        Path jobRoot = moduleDir.resolve("output/test_job_translate").normalize().toAbsolutePath();
        Path transcript = jobRoot.resolve("text/source_transcript.txt");
        Files.createDirectories(transcript.getParent());
        Files.writeString(transcript, "Hello. This is a translation integration test.");

        TranslationService service = new TranslationService();
        Map<String, String> translations;
        try {
            translations = service.process(transcript.toString());
        } catch (PipelineException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            assumeTrue(
                    !(msg.contains("connection") || msg.contains("network") || msg.contains("timed out")),
                    "Skipping due to transient network issue: " + e.getMessage()
            );
            throw e;
        }

        assertNotNull(translations);
        assertTrue(translations.containsKey("ro"));

        Path roFile = Path.of(translations.get("ro"));
        assertTrue(Files.exists(roFile), "Romanian translation file not created");
        String roText = Files.readString(roFile);
        assertFalse(roText.isBlank(), "Romanian translation output is empty");
    }

    private static boolean canRunPython(String pythonBin) {
        try {
            Process process = new ProcessBuilder(pythonBin, "--version").start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

