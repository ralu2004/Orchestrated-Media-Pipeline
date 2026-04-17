package app.common;

/** Exception type used to report pipeline-stage failures with stage metadata. */
public class PipelineException extends Exception {
    private final String stageName;

    public PipelineException(String message, String stageName, Throwable cause) {
        super(message, cause);
        this.stageName = stageName;
    }

    public PipelineException(String message, String stageName) {
        super(message);
        this.stageName = stageName;
    }

    public PipelineException(String message, PipelineStageName stage, Throwable cause) {
        this(message, stage.name(), cause);
    }

    public PipelineException(String message, PipelineStageName stage) {
        this(message, stage.name());
    }

    public String getStageName() {
        return stageName;
    }
}
