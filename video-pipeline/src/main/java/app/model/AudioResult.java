package app.model;

import java.util.Map;

public record AudioResult(String transcriptPath, Map<String, String> translations, Map<String, String> syntheticAudio) {}