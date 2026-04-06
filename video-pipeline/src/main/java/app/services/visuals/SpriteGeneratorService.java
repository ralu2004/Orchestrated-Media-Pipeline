package app.services.visuals;

import app.common.PipelineException;
import app.common.PipelineStage;
import app.model.VisualsContext;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Generates a sprite map (filmstrip) from the source video using ffmpeg.
 *
 * Captures one frame every 10 seconds, scales each to 160x90,
 * and tiles them into a single sprite_map.jpg used for scrubbing preview.
 */
public class SpriteGeneratorService implements PipelineStage<VisualsContext, String> {

    @Override
    public String process(VisualsContext input) throws PipelineException {
        try {
            String outputPath = buildOutputPath(input.jobRequest().jobId());
            Files.createDirectories(Paths.get(outputPath).getParent());
            runFfmpeg(input.jobRequest().sourceFile(), outputPath);
            return outputPath;
        } catch (PipelineException e) {
            throw e; 
        } catch (Exception e) {
            throw new PipelineException("Sprite generation failed", "PROCESSING", e);
        }
    }

    private void runFfmpeg(String sourceFile, String outputPath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", sourceFile,
                "-vf", "fps=1/10,scale=160:90,tile=10x10",
                outputPath
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new PipelineException("ffmpeg sprite generation failed with exit code " + exitCode + "\n" + output, "PROCESSING");
        }
    }

    private String buildOutputPath(String jobId) {
        return String.format("output/%s/images/sprite_map.jpg", jobId);
    }
}