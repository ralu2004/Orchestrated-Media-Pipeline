package app.model;

import java.util.List;

public record VisualsContext(JobRequest jobRequest, EncodingProfile encodingProfile, List<TranscodedVideo> transcodedVideos) {}
