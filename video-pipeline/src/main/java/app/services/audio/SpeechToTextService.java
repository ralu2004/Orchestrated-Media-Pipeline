package app.services.audio;

import app.common.FfmpegRunner;
import app.common.PipelineException;
import app.common.PipelineStage;
import app.model.AudioContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Extracts audio from the source video, then transcribes it via {@code scripts/transcribe.py}.
 *
 * Writes the transcript to {@code output/{jobId}/text/source_transcript.txt} and returns that path.
 */
public class SpeechToTextService implements PipelineStage<AudioContext, String> {

    private final FfmpegRunner runner;

    public SpeechToTextService(FfmpegRunner runner) {
        this.runner = runner;
    }

    @Override
    public String process(AudioContext input) throws PipelineException {
        String tempAudioPath = null;
        try {
            tempAudioPath = extractAudio(input.jobRequest().sourceFile(), input.jobRequest().jobId());
            String transcriptPath = buildTranscriptPath(input.jobRequest().jobId());
            transcribe(tempAudioPath, transcriptPath);
            return transcriptPath;
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Speech to text failed", "PROCESSING", e);
        } finally {
            if (tempAudioPath != null) {
                try { Files.deleteIfExists(Paths.get(tempAudioPath)); }
                catch (Exception ignored) {}
            }
        }
    }

    private String extractAudio(String sourceFile, String jobId) throws PipelineException {
        String tempAudioPath = System.getProperty("java.io.tmpdir") + "/" + jobId + "_temp_audio.mp3";
        runner.runCaptureStderr(List.of(
                "ffmpeg", "-y",
                "-i", sourceFile,
                "-vn", "-ac", "1", "-ar", "16000",
                "-f", "mp3",
                tempAudioPath
        ));
        return tempAudioPath;
    }

    private String buildTranscriptPath(String jobId) {
        return Path.of("output", jobId, "text", "source_transcript.txt").toString();
    }

    private void transcribe(String audioPath, String transcriptPath) throws PipelineException {
        String pythonBin = System.getenv("PYTHON_BIN");
        if (pythonBin == null || pythonBin.isBlank()) {
            pythonBin = "python";
        }

        String scriptPath = Path.of("scripts", "transcribe.py").toString();
        ProcessBuilder pb = new ProcessBuilder(pythonBin, scriptPath, audioPath, transcriptPath);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new PipelineException("Python transcription failed: " + output, "PROCESSING");
            }
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Python transcription failed", "PROCESSING", e);
        }
    }
}
