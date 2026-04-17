package app.model;

import java.util.List;

/** Outputs of visuals processing: profile, renditions, sprite map, and thumbnails. */
public record VisualsResult(
        EncodingProfile encodingProfile,
        List<TranscodedVideo> transcodedVideos,
        String spriteMapPath,
        List<String> thumbnailPaths) {}