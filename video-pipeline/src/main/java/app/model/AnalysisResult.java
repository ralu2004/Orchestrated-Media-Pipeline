package app.model;

import java.util.List;

/** Aggregated outputs produced by the analysis phase. */
public record AnalysisResult(String introEnd, String outroStart, String creditsTimestamp, List<SceneSegment> segments) {
    public AnalysisResult {
        if (introEnd == null || introEnd.isBlank()) {
            throw new IllegalArgumentException("Intro end must not be blank");
        }
        if (outroStart == null || outroStart.isBlank()) {
            throw new IllegalArgumentException("Outro start must not be blank");
        }
        if (creditsTimestamp == null || creditsTimestamp.isBlank()) {
            throw new IllegalArgumentException("Credits timestamp must not be blank");
        }
        if (segments == null) {
            throw new IllegalArgumentException("Segments must not be null");
        }
    }
}