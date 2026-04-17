package app.model;

import java.util.List;

/** Input bundle for visuals processing substages. */
public record VisualsContext(JobRequest jobRequest, EncodingProfile encodingProfile, List<TranscodedVideo> transcodedVideos) {}
