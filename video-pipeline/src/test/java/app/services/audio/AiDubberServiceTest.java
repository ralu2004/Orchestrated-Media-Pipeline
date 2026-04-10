package app.services.audio;

import app.common.PipelineException;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AiDubberServiceTest {

    @Test
    void generates_synthetic_audio_file_for_ro_translation() throws Exception {
        String pythonBin = System.getenv("PYTHON_BIN");
        if (pythonBin == null || pythonBin.isBlank()) {
            pythonBin = "python";
        }
        assumeTrue(canRunPython(pythonBin), "Python is not available: " + pythonBin);

        Path moduleDir = Path.of("").toAbsolutePath();
        Path script = moduleDir.resolve("scripts/dub.py").normalize().toAbsolutePath();
        assumeTrue(Files.exists(script), "dub.py not found at " + script);

        Path jobRoot = moduleDir.resolve("output/test_job_dub").normalize().toAbsolutePath();
        Path roTranslation = jobRoot.resolve("text/ro_translation.txt");
        Files.createDirectories(roTranslation.getParent());
        Files.writeString(roTranslation, "Acesta este un test pentru voce sintetica.");

        AiDubberService service = new AiDubberService();
        Map<String, String> syntheticAudio;
        try {
            syntheticAudio = service.process(Map.of("ro", roTranslation.toString()));
        } catch (PipelineException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            assumeTrue(
                    !(msg.contains("connection") || msg.contains("network") || msg.contains("timed out")),
                    "Skipping due to transient network issue: " + e.getMessage()
            );
            throw e;
        }

        assertNotNull(syntheticAudio);
        assertTrue(syntheticAudio.containsKey("ro"));

        Path roAudio = Path.of(syntheticAudio.get("ro"));
        assertTrue(Files.exists(roAudio), "Romanian dubbed audio file not created");
        assertTrue(Files.size(roAudio) > 0, "Romanian dubbed audio file is empty");
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

