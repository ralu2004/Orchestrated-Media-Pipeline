package app.model;

/** Input bundle for the analysis phase. */
public record AnalysisContext(JobRequest jobRequest, IngestResult ingestResult) {
}
