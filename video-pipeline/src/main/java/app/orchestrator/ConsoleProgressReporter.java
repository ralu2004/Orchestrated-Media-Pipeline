package app.orchestrator;

import app.common.PipelineException;
import app.model.JobStatus;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Console reporter: timestamps each line ({@code HH:mm:ss}), prints stage start/finish and
 * per-stage duration to {@link System#out}, failures to {@link System#err}, and total run time on
 * {@link #onPipelineCompleted(long)}.
 */
public final class ConsoleProgressReporter implements ProgressReporter {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static String timestamp() {
        return LocalTime.now().format(TIME);
    }

    private static String formatElapsed(long elapsedMs) {
        return String.format(Locale.US, "%.1fs", elapsedMs / 1000.0);
    }

    @Override
    public void onStageStarted(JobStatus stage) {
        System.out.println("[" + timestamp() + "] Starting " + stage.name() + "...");
    }

    @Override
    public void onStageCompleted(JobStatus stage, long elapsedMs) {
        System.out.println("[" + timestamp() + "] " + stage.name() + " completed in " + formatElapsed(elapsedMs));
    }

    @Override
    public void onFailed(JobStatus stage, PipelineException cause) {
        String msg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        System.err.println("[" + timestamp() + "] " + stage.name() + " failed: " + msg);
    }

    @Override
    public void onPipelineCompleted(long totalElapsedMs) {
        System.out.println("[" + timestamp() + "] Pipeline completed in " + formatElapsed(totalElapsedMs));
    }
}
