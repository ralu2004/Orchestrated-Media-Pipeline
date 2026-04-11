package app.common;

/**
 * Labels for {@link PipelineException#getStageName()} and {@link FfmpegRunner} attribution.
 * {@link #PROCESSING} is the coarse job phase; {@link #AUDIO} and {@link #VISUALS} refine parallel work under it.
 */
public enum PipelineStageName {
    INGESTING,
    ANALYZING,
    PROCESSING,
    AUDIO,
    VISUALS,
    COMPLIANCE,
    PACKAGING,
    ORCHESTRATION,
    UNKNOWN
}
