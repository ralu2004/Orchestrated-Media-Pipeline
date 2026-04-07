package app.services.analysis;

public record RawAnalysisData(
        double durationSeconds,
        String silenceOutput,
        String sceneOutput,
        String blackdetectOutput
) {
}

