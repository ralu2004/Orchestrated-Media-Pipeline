package app.model;

public record PackagingContext(
        JobRequest jobRequest,
        VisualsResult visualsResult,
        AudioResult audioResult,
        ComplianceResult complianceResult
) {
}
