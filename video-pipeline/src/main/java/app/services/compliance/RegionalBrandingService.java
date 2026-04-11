package app.services.compliance;

import app.common.FfmpegRunner;
import app.common.PipelineException;
import app.common.PipelineStageName;
import app.model.ComplianceContext;
import app.model.TranscodedVideo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies a branding logo overlay to each transcoded video.
 
 * Generates a small PNG via ffmpeg lavfi, then overlays it on the top-right of each video.
 */
public class RegionalBrandingService {

    private final FfmpegRunner runner;

    public RegionalBrandingService(FfmpegRunner runner) {
        this.runner = runner;
    }

    public List<TranscodedVideo> apply(ComplianceContext context) throws PipelineException {
        String jobId = context.jobRequest().jobId();
        var videos = context.visualsResult().transcodedVideos();
        if (videos == null || videos.isEmpty()) {
            return List.of();
        }

        Path logoPath = Path.of(System.getProperty("java.io.tmpdir"), jobId + "_branding_logo.png");
        try {
            generateLogoPng(logoPath);

            List<TranscodedVideo> branded = new ArrayList<>();
            for (TranscodedVideo v : videos) {
                Path dest = Path.of(v.path()).normalize();
                Files.createDirectories(dest.getParent());
                Path tempOut = dest.resolveSibling(dest.getFileName().toString() + ".branding.tmp");
                try {
                    overlayLogo(ffmpegPath(dest.toString()), ffmpegPath(logoPath.toString()), ffmpegPath(tempOut.toString()), v.codec());
                    Files.move(tempOut, dest, StandardCopyOption.REPLACE_EXISTING);
                } catch (PipelineException e) {
                    Files.deleteIfExists(tempOut);
                    throw e;
                }
                branded.add(new TranscodedVideo(v.path(), v.codec(), v.resolution()));
            }
            return branded;
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Regional branding failed", PipelineStageName.COMPLIANCE, e);
        } finally {
            try {
                Files.deleteIfExists(logoPath);
            } catch (Exception ignored) {
            }
        }
    }

    private void generateLogoPng(Path logoPath) throws PipelineException {
        runner.runCaptureStderr(List.of(
                "ffmpeg", "-y",
                "-f", "lavfi", "-i", "color=c=0xE50914@0.92:s=320x88:d=1",
                "-frames:v", "1",
                ffmpegPath(logoPath)));
    }

    private void overlayLogo(String inputVideo, String logoPng, String outputVideo, String codec)
            throws PipelineException {
        String filter = "[1:v]format=rgba,scale=w=280:h=-1[lg];[0:v][lg]overlay=x=W-w-24:y=24[vout]";

        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(ffmpegPath(inputVideo));
        cmd.add("-i");
        cmd.add(logoPng);
        cmd.add("-filter_complex");
        cmd.add(filter);
        cmd.add("-map");
        cmd.add("[vout]");
        appendAudioOptions(cmd, outputVideo);
        appendVideoEncoder(cmd, codec);
        if ("vp9".equals(codec) || "hevc".equals(codec)) {
            cmd.addAll(List.of("-pix_fmt", "yuv420p"));
        }
        cmd.add(ffmpegPath(outputVideo));

        runner.runCaptureStderr(cmd);
    }

    private static String ffmpegPath(String path) {
        return path.replace('\\', '/');
    }

    private static String ffmpegPath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static void appendAudioOptions(List<String> cmd, String outputVideo) {
        cmd.add("-map");
        cmd.add("0:a?");
        String out = outputVideo.toLowerCase();
        if (out.endsWith(".webm")) {
            cmd.addAll(List.of("-c:a", "libopus", "-b:a", "128k"));
        } else {
            cmd.addAll(List.of("-c:a", "copy"));
        }
    }

    private void appendVideoEncoder(List<String> cmd, String codec) {
        switch (codec) {
            case "h264" -> cmd.addAll(List.of("-c:v", "libx264", "-preset", "fast", "-crf", "23"));
            case "vp9" -> cmd.addAll(List.of("-c:v", "libvpx-vp9", "-crf", "35", "-b:v", "0"));
            case "hevc" -> cmd.addAll(List.of("-c:v", "libx265", "-crf", "28"));
            default -> cmd.addAll(List.of("-c:v", "libx264", "-preset", "fast", "-crf", "23"));
        }
    }
}
