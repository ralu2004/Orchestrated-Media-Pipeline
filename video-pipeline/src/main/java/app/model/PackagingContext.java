package app.model;

/** Input bundle for the packaging phase. */
public record PackagingContext(
        JobRequest jobRequest,
        VisualsResult visualsResult,
        AudioResult audioResult,
        ComplianceResult complianceResult
) {
}
