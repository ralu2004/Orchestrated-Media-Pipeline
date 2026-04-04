package app.common;

public interface PipelineStage<I, O> {
    O process(I input) throws PipelineException;
}