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
    private static final String SPRITE_VIDEO_FILTER = "fps=1/10,scale=160:90,tile=10x10";
    private static final int THUMB_WIDTH_PX = 320;

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
        } catch (IOException e) {
            throw new PipelineException("Sprite / thumbnail I/O failed", "PROCESSING", e);
        }
    }

    private void runSpriteFfmpeg(String sourceFile, String outputPath) throws PipelineException {
        runner.runCaptureStderr(List.of(
                "ffmpeg", "-y",
                "-i", sourceFile,
                "-vf", SPRITE_VIDEO_FILTER,
                outputPath
        ));
    }

    private List<String> generateThumbnails(VisualsContext input) throws IOException, PipelineException {
        String jobId = input.jobRequest().jobId();
        Path dir = Path.of(String.format("output/%s/images/thumbnails", jobId));
        Files.createDirectories(dir);

        String pattern = dir.resolve("thumb_%04d.jpg").toString().replace('\\', '/');
        String thumbFilter = "fps=1/" + THUMB_INTERVAL_SECONDS + ",scale=" + THUMB_WIDTH_PX + ":-1";

        runner.runCaptureStderr(List.of(
                "ffmpeg", "-y",
                "-i", input.jobRequest().sourceFile(),
                "-vf", thumbFilter,
                pattern));

        try (Stream<Path> list = Files.list(dir)) {
            List<String> paths = new ArrayList<>();
            list.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".jpg"))
                    .sorted()
                    .forEach(p -> paths.add(p.toString().replace('\\', '/')));
            return Collections.unmodifiableList(paths);
        }
    }

    private String buildSpritePath(String jobId) {
        return String.format("output/%s/images/sprite_map.jpg", jobId);
    }
}
