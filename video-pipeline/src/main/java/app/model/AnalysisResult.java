package app.model;

import java.util.List;

public record AnalysisResult(String introTimestamp, String outroTimestamp, String creditsTimestamp, List<SceneSegment> segments) {}