package app.model;

import java.util.Map;

/** Outputs of the audio phase: transcript, translations, and dubbed tracks. */
public record AudioResult(String transcriptPath, Map<String, String> translations, Map<String, String> syntheticAudio) {}