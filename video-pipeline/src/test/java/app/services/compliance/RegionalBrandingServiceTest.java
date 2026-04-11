package app.services.compliance;

import app.common.FfmpegRunner;
import app.common.PipelineStageName;
import app.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RegionalBrandingServiceTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void requireFfmpeg() throws Exception {
        assumeTrue(ffmpegAvailable(), "ffmpeg not on PATH");
    }

    private static boolean ffmpegAvailable() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
    }

    @Test
    void branding_overlays_logo_without_single_frame_overlay_failure() throws Exception {
        Path moduleDir = Path.of("").toAbsolutePath();
        Path sampleVideo = moduleDir.resolve("../samples/input.mp4").normalize().toAbsolutePath();
        assumeTrue(Files.exists(sampleVideo), "Sample video not found at " + sampleVideo);

        Path input = tempDir.resolve("input.mp4");
        Files.copy(sampleVideo, input);

        long sizeBefore = Files.size(input);

        TranscodedVideo clip = new TranscodedVideo(input.toAbsolutePath().normalize().toString(), "h264", "720p");
        EncodingProfile profile = new EncodingProfile(1_000_000, 23);
        VisualsResult visuals = new VisualsResult(profile, List.of(clip), "unused", List.of());
        JobRequest job = new JobRequest("branding_smoke", input.toString(), null);
        ComplianceContext ctx = new ComplianceContext(job, visuals);

        RegionalBrandingService branding = new RegionalBrandingService(new FfmpegRunner(PipelineStageName.COMPLIANCE));
        List<TranscodedVideo> out = branding.apply(ctx);

        assertEquals(1, out.size());
        assertNotEquals(clip.path(), out.getFirst().path());
        assertTrue(out.getFirst().path().contains("compliance"),
                "Expected branded output under compliance/: " + out.getFirst().path());
        assertTrue(Files.exists(Path.of(out.getFirst().path())),
                "Branded file does not exist: " + out.getFirst().path());
        assertTrue(Files.size(Path.of(out.getFirst().path())) > 0,
                "Branded file is empty");

        assertTrue(Files.exists(input), "Original video should still exist");
        assertEquals(sizeBefore, Files.size(input), "Original file should not be modified");
    }
}