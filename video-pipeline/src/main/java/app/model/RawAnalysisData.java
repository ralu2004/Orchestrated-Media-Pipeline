package app.model;

public record RawAnalysisData(
        double durationSeconds,
        String silenceOutput,
        String sceneOutput,
        String blackdetectOutput
) {
}

