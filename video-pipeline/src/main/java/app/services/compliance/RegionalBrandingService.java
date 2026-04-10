package app.services.compliance;

import app.common.FfmpegRunner;
import app.common.PipelineException;
import app.model.ComplianceContext;
import app.model.TranscodedVideo;

import java.nio.file.Files;
import java.nio.file.Path;
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

        Path logoPath = Path.of(System.getProperty("java.io.tmpdir"), jobId + "_compliance_logo.png");
        try {
            generateLogoPng(logoPath);

            List<TranscodedVideo> branded = new ArrayList<>();
            for (TranscodedVideo v : videos) {
                String outPath = buildBrandedPath(jobId, v);
                Files.createDirectories(Path.of(outPath).getParent());
                overlayLogo(v.path(), logoPath.toString(), outPath, v.codec());
                branded.add(new TranscodedVideo(outPath, v.codec(), v.resolution()));
            }
            return branded;
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Regional branding failed", "COMPLIANCE", e);
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
                logoPath.toString()));
    }

    private String buildBrandedPath(String jobId, TranscodedVideo v) {
        String fileName = Path.of(v.path()).getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot + 1) : "mp4";
        return String.format("output/%s/compliance/video/%s/%s_branded.%s",
                jobId, v.codec(), base, ext);
    }

    private void overlayLogo(String inputVideo, String logoPng, String outputVideo, String codec)
            throws PipelineException {
        String filter = "[1:v]format=rgba,scale=w=280:h=-1[lg];[0:v][lg]overlay=x=W-w-24:y=24:format=auto[vout]";

        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(inputVideo);
        cmd.add("-i");
        cmd.add(logoPng);
        cmd.add("-filter_complex");
        cmd.add(filter);
        cmd.add("-map");
        cmd.add("[vout]");
        cmd.add("-map");
        cmd.add("0:a?");
        cmd.add("-c:a");
        cmd.add("copy");
        appendVideoEncoder(cmd, codec);
        cmd.add(outputVideo);

        runner.runCaptureStderr(cmd);
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
