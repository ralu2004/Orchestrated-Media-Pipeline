package app.common;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Executes ffmpeg/ffprobe subprocesses with safe stream handling, timeouts,
 * UTF-8 decoding, and bounded output capture.
 */
public final class FfmpegRunner {

    private static final Duration TIMEOUT = Duration.ofMinutes(30);
    private static final int MAX_CAPTURE_CHARS = 1_000_000;
    private static final int ERROR_SNIPPET_CHARS = 2000;

    private final String stageName;

    public FfmpegRunner(String stageName) {
        this.stageName = stageName;
    }

    public String run(List<String> command) throws PipelineException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process;
        try {
            process = pb.start();
        } catch (Exception e) {
            throw new PipelineException("Failed to start process", stageName, e);
        }

        String combined = readStreamLimited(process.getInputStream(), MAX_CAPTURE_CHARS);
        int exitCode = waitForOrTimeout(process);
        if (exitCode != 0) {
            throw new PipelineException(buildExitMessage(exitCode, combined), stageName);
        }
        return combined;
    }

    public String runCaptureStderr(List<String> command) throws PipelineException {
        ProcessBuilder pb = new ProcessBuilder(command);

        Process process;
        try {
            process = pb.start();
        } catch (Exception e) {
            throw new PipelineException("Failed to start process", stageName, e);
        }

        Thread drainStdout = new Thread(() -> drainFully(process.getInputStream()), "ffmpeg-stdout-drain");
        drainStdout.setDaemon(true);
        drainStdout.start();

        String stderr = readStreamLimited(process.getErrorStream(), MAX_CAPTURE_CHARS);
        int exitCode = waitForOrTimeout(process);
        if (exitCode != 0) {
            throw new PipelineException(buildExitMessage(exitCode, stderr), stageName);
        }
        return stderr;
    }

    private int waitForOrTimeout(Process process) throws PipelineException {
        boolean finished;
        try {
            finished = process.waitFor(TIMEOUT.toMinutes(), TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new PipelineException("Process interrupted", stageName, e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new PipelineException("Process timed out", stageName);
        }
        return process.exitValue();
    }

    private static void drainFully(InputStream in) {
        try {
            byte[] buf = new byte[8192];
            while (in.read(buf) != -1) {
                // discard
            }
        } catch (Exception ignored) {
        }
    }

    private String readStreamLimited(InputStream in, int maxChars) throws PipelineException {
        StringBuilder sb = new StringBuilder(Math.min(maxChars, 256));
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            char[] buf = new char[1024];
            int read;
            while ((read = br.read(buf)) != -1) {
                int remaining = maxChars - sb.length();
                if (remaining <= 0) break;
                sb.append(buf, 0, Math.min(read, remaining));
            }
        } catch (Exception e) {
            throw new PipelineException("Failed to read process output", stageName, e);
        }
        return sb.toString();
    }

    private static String buildExitMessage(int exitCode, String output) {
        String snippet = truncate(output, ERROR_SNIPPET_CHARS);
        String suffix = snippet == null || snippet.isBlank() ? "" : ("\n" + snippet);
        return "Process failed with exit code " + exitCode + suffix;
    }

    private static String truncate(String s, int maxChars) {
        if (s == null) return null;
        if (s.length() <= maxChars) return s;
        return s.substring(0, maxChars);
    }
}

