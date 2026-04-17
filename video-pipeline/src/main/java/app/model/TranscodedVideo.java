package app.model;

/** Descriptor for one transcoded video rendition. */
public record TranscodedVideo(String path, String codec, String resolution) {}