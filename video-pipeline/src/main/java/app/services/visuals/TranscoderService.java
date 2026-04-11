package app.services.visuals;

import app.common.FfmpegRunner;
import app.common.PipelineException;
import app.common.PipelineStageName;
import app.common.PipelineStage;
import app.model.TranscodedVideo;
import app.model.VisualsContext;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Transcodes the source video into 9 output files using ffmpeg:
 * 3 codecs (h264, vp9, hevc) x 3 resolutions (4K, 1080p, 720p).
 *
 * Encoding parameters (bitrate, crf) are driven by the EncodingProfile
 * produced by SceneComplexityService.
 */
public class TranscoderService implements PipelineStage<VisualsContext, List<TranscodedVideo>> {

    private final FfmpegRunner runner;

    public TranscoderService(FfmpegRunner runner) {
        this.runner = runner;
    }

    private static final String[][] CODECS = {
            {"libx264", "h264", "mp4"},
            {"libvpx-vp9", "vp9", "webm"},
            {"libx265", "hevc", "mkv"}
    };

    private static final String[][] RESOLUTIONS = {
            {"4k", "3840:2160"},
            {"1080p", "1920:1080"},
            {"720p", "1280:720"}
    };

    @Override
    public List<TranscodedVideo> process(VisualsContext input) throws PipelineException {
        List<TranscodedVideo> results = new ArrayList<>();
        try {
            for (String[] codec : CODECS) {
                for (String[] resolution : RESOLUTIONS) {
                    TranscodedVideo video = transcode(input, codec, resolution);
                    results.add(video);
                }
            }
        } catch (Exception e) {
            throw new PipelineException("Transcoding failed", PipelineStageName.VISUALS, e);
        }
        return results;
    }

    private TranscodedVideo transcode(VisualsContext input, String[] codec, String[] resolution) throws Exception {
        String outputPath = buildOutputPath(input.jobRequest().jobId(), codec, resolution);

        Files.createDirectories(Paths.get(outputPath).getParent());

        List<String> command = new ArrayList<>();

        command.add("ffmpeg");
        command.add("-y"); // overwrite output

        //input
        command.add("-i");
        command.add(input.jobRequest().sourceFile());

        //video codec
        command.add("-c:v");
        command.add(codec[0]);

        //quality
        command.add("-crf");
        command.add(String.valueOf(input.encodingProfile().quality()));

        //bitrate
        command.add("-b:v");
        command.add(String.valueOf(input.encodingProfile().bitrate()));

        command.add("-vf");
        command.add("scale=" + resolution[1]);

        if (codec[0].equals("libx264") || codec[0].equals("libx265")) {
            command.add("-preset");
            command.add("medium");
        }

        if (codec[0].equals("libvpx-vp9")) {
            command.add("-deadline");
            command.add("good");
            command.add("-cpu-used");
            command.add("2");
        }

        // limit to 2 threads per ffmpeg process
        command.add("-threads");
        command.add("2");
        command.add(outputPath);

        runner.runCaptureStderr(command);

        return new TranscodedVideo(outputPath, codec[1], resolution[0]);
    }


    private String buildOutputPath(String jobId, String[] codec, String[] resolution) {
        return String.format(
                "output/%s/video/%s/%s_%s.%s",
                jobId,
                codec[1],
                resolution[0],
                codec[1],
                codec[2]
        );
    }

}