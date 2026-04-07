package app.model;

import java.util.List;

public record AnalysisResult(IntroOutroTimestamps introOutro, String creditsTimestamp, List<SceneSegment> segments) {}