package app.model;

import java.util.List;

public record VisualsResult(
        EncodingProfile encodingProfile,
        List<TranscodedVideo> transcodedVideos,
        String spriteMapPath,
        List<String> thumbnailPaths) {}