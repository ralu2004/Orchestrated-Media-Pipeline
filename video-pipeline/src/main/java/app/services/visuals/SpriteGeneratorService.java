package app.services.visuals;

import app.common.FfmpegRunner;
import app.common.PipelineException;
import app.common.PipelineStage;
import app.model.VisualsContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Generates the scrub sprite map and periodic thumbnail JPEGs under {@code images/} using ffmpeg.
 */
public class SpriteGeneratorService implements PipelineStage<VisualsContext, SpriteGeneratorService.Artifacts> {

    private static final int THUMB_INTERVAL_SECONDS = 10;

    private final FfmpegRunner runner;

    public SpriteGeneratorService(FfmpegRunner runner) {
        this.runner = runner;
    }

    public record Artifacts(String spriteMapPath, List<String> thumbnailPaths) {}

    @Override
    public Artifacts process(VisualsContext input) throws PipelineException {
        try {
            String spritePath = buildSpritePath(input.jobRequest().jobId());
            Files.createDirectories(Paths.get(spritePath).getParent());
            runSpriteFfmpeg(input.jobRequest().sourceFile(), spritePath);

            List<String> thumbnailPaths = generateThumbnails(input);
            return new Artifacts(spritePath, thumbnailPaths);
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Sprite / thumbnail generation failed", "PROCESSING", e);
        }
    }

    private void runSpriteFfmpeg(String sourceFile, String outputPath) throws Exception {
        runner.runCaptureStderr(List.of(
                "ffmpeg", "-y",
                "-i", sourceFile,
                "-vf", "fps=1/10,scale=160:90,tile=10x10",
                outputPath
        ));
    }

    private List<String> generateThumbnails(VisualsContext input) throws Exception {
        String jobId = input.jobRequest().jobId();
        Path dir = Path.of(String.format("output/%s/images/thumbnails", jobId));
        Files.createDirectories(dir);

        String pattern = dir.resolve("thumb_%04d.jpg").toString().replace('\\', '/');
        String fps = "fps=1/" + THUMB_INTERVAL_SECONDS;

        runner.runCaptureStderr(List.of(
                "ffmpeg", "-y",
                "-i", input.jobRequest().sourceFile(),
                "-vf", fps + ",scale=320:-1",
                pattern));

        try (Stream<Path> list = Files.list(dir)) {
            List<String> paths = new ArrayList<>();
            list.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".jpg"))
                    .sorted()
                    .forEach(p -> paths.add(p.toString().replace('\\', '/')));
            return Collections.unmodifiableList(paths);
        } catch (IOException e) {
            throw new PipelineException("Failed to list thumbnails", "PROCESSING", e);
        }
    }

    private String buildSpritePath(String jobId) {
        return String.format("output/%s/images/sprite_map.jpg", jobId);
    }
}
