package app.common;

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

    public String getStageName() {
        return stageName;
    }
}
