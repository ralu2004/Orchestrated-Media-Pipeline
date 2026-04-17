package app.common;

/** Generic contract for a pipeline stage that transforms input into output. */
public interface PipelineStage<I, O> {
    O process(I input) throws PipelineException;
}