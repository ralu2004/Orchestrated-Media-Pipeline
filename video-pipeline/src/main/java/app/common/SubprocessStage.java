package app.common;

import java.util.List;

public abstract class SubprocessStage<I, O> implements PipelineStage<I, O> {

    protected String runProcess(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new PipelineException("Process failed with exit code " + exitCode + "\n" + output, getStageName());
        }
        return output;
    }

    protected abstract String getStageName();
}