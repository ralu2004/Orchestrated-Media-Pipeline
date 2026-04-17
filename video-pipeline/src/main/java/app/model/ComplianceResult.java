package app.model;

import java.util.List;

/** Compliance outputs: content flags and branded delivery videos. */
public record ComplianceResult(List<ContentFlag> flags, List<TranscodedVideo> processedVideos) {}