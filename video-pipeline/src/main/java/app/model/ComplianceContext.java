package app.model;

/** Input bundle for the compliance phase. */
public record ComplianceContext(JobRequest jobRequest, VisualsResult visualsResult) {
}
