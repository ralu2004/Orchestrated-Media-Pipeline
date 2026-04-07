package app.model;

import java.util.List;

public record AnalysisResult(String introEnd, String outroStart, String creditsTimestamp, List<SceneSegment> segments) {}