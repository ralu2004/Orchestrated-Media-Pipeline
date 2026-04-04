package app.model;

import java.util.List;

public record ComplianceResult(List<ContentFlag> flags, List<TranscodedVideo> processedVideos) {}