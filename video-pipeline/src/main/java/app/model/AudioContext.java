package app.model;

/** Input bundle for the audio/text processing phase. */
public record AudioContext(JobRequest jobRequest, AnalysisResult analysisResult) {
}
