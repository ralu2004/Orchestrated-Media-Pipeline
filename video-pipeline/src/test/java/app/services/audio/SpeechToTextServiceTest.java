package app.services.audio;

import app.common.FfmpegRunner;
import app.common.PipelineStageName;
import app.model.AnalysisResult;
import app.model.AudioContext;
import app.model.JobRequest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SpeechToTextServiceTest {

    @Test
    void transcribes_sample_video_with_python() throws Exception {
        String pythonBin = System.getenv("PYTHON_BIN");
        if (pythonBin == null || pythonBin.isBlank()) {
            pythonBin = "python";
        }
        String finalPythonBin = pythonBin;
        assumeTrue(canRunPython(finalPythonBin), "Python is not available: " + finalPythonBin);

        Path moduleDir = Path.of("").toAbsolutePath();
        Path sampleVideo = moduleDir.resolve("../samples/input.mp4").normalize().toAbsolutePath();
        assumeTrue(Files.exists(sampleVideo), "Sample video not found at " + sampleVideo);
        Path script = moduleDir.resolve("scripts/transcribe.py").normalize().toAbsolutePath();
        assumeTrue(Files.exists(script), "transcribe.py not found at " + script);

        JobRequest request = new JobRequest("test_job_stt", sampleVideo.toString(), null);
        AnalysisResult analysisResult = new AnalysisResult("00:00:01", "00:00:10", "00:00:09", List.of());
        AudioContext context = new AudioContext(request, analysisResult);

        SpeechToTextService service = new SpeechToTextService(new FfmpegRunner(PipelineStageName.PROCESSING));
        String transcriptPath = service.process(context);
        Path transcriptFile = Path.of(transcriptPath);

        assertNotNull(transcriptPath);
        assertFalse(transcriptPath.isBlank(), "Expected non-empty transcript path");
        assertTrue(Files.exists(transcriptFile), "Transcript output file was not created");
        String transcript = Files.readString(transcriptFile);
        assertFalse(transcript.isBlank(), "Expected non-empty transcription");
    }

    private static boolean canRunPython(String pythonBin) {
        try {
            Process process = new ProcessBuilder(pythonBin, "--version").start();
            int code = process.waitFor();
            return code == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

