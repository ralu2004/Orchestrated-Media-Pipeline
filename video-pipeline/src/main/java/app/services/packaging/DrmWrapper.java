package app.services.packaging;

import app.common.FfmpegRunner;
import app.common.PipelineException;
import app.model.TranscodedVideo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Encrypts each delivery video using HLS AES-128 encryption.
 *
 * Each video is split into 10-second segments (.ts files) and encrypted
 * with a shared AES-128 key. A .m3u8 playlist is generated per codec
 * that references the encrypted segments and the key URL.
 *
 * The output is playable in VLC and other HLS-capable players.
 *
 */
public class DrmWrapper {

    private static final int HLS_SEGMENT_SECONDS = 10;

    private final FfmpegRunner runner;

    public DrmWrapper(FfmpegRunner runner) {
        this.runner = runner;
    }

    public List<String> wrapDeliveryAssets(String jobId, List<TranscodedVideo> deliveryVideos)
            throws IOException, PipelineException {
        Path drmRoot = Path.of("output", jobId, "drm");
        Files.createDirectories(drmRoot);

        Path keyPath = generateKey(drmRoot);
        Path keyInfoPath = generateKeyInfo(drmRoot, keyPath);

        List<String> playlists = new ArrayList<>();
        for (TranscodedVideo v : deliveryVideos) {
            Path segmentDir = drmRoot.resolve(v.codec());
            Files.createDirectories(segmentDir);
            encryptToHls(v.path(), keyInfoPath, segmentDir);
            playlists.add(ffmpegPath(segmentDir.resolve("playlist.m3u8")));
        }
        return playlists;
    }

    /**
     * Generates a 16-byte AES-128 key and writes it to disk.
     */
    private Path generateKey(Path drmRoot) throws IOException {
        byte[] keyBytes = new byte[16];
        new SecureRandom().nextBytes(keyBytes);
        Path keyPath = drmRoot.resolve("encryption.key");
        Files.write(keyPath, keyBytes);
        return keyPath;
    }

    /**
     * Creates the ffmpeg keyinfo file with three lines:
     *   1. Key URL — where a player would fetch the key (as a placeholder for now)
     *   2. Local key path — where ffmpeg reads the key from
     *   3. IV — blank (ffmpeg auto-generates per segment)
     */
    private Path generateKeyInfo(Path drmRoot, Path keyPath) throws IOException {
        Path keyInfoPath = drmRoot.resolve("encryption.keyinfo");
        String content = String.join("\n",
                "file:///" + ffmpegPath(keyPath),   // key URL
                ffmpegPath(keyPath),                         // local path
                ""                                           // IV
        );
        Files.writeString(keyInfoPath, content);
        return keyInfoPath;
    }

    /**
     * Splits and encrypts a single video into HLS segments using ffmpeg.
     */
    private void encryptToHls(String sourceFile, Path keyInfoPath, Path segmentDir)
            throws PipelineException {
        Path playlist = segmentDir.resolve("playlist.m3u8");
        String segmentPattern = ffmpegPath(segmentDir.resolve("segment_%03d.ts"));

        runner.runCaptureStderr(List.of(
                "ffmpeg", "-y",
                "-i", ffmpegPath(sourceFile),
                "-c", "copy",                           // no re-encoding
                "-hls_time", String.valueOf(HLS_SEGMENT_SECONDS),
                "-hls_key_info_file", ffmpegPath(keyInfoPath),
                "-hls_playlist_type", "vod",            // full playlist, not live
                "-hls_segment_filename", segmentPattern,
                ffmpegPath(playlist)
        ));
    }

    private static String ffmpegPath(String path) {
        return path.replace('\\', '/');
    }

    private static String ffmpegPath(Path path) {
        return path.toString().replace('\\', '/');
    }
}