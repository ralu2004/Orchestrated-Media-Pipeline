package app.model;

/** Encoding parameters derived from complexity analysis and used by transcoding. */
public record EncodingProfile(int bitrate, int quality) {
    public EncodingProfile {
        if (bitrate <= 0) {
            throw new IllegalArgumentException("Bitrate must be positive");
        }
        if (quality < 0 || quality > 51) {
            throw new IllegalArgumentException("Quality must be between 0 and 51");
        }
    }
}