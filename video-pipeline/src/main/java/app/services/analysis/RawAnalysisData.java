package app.services.analysis;

/**
 * Raw ffmpeg filter outputs captured once per job for analysis substages to parse.
 */
public record RawAnalysisData(
        double durationSeconds,
        String silenceOutput,
        String sceneOutput,
        String blackdetectOutput
) {
}

