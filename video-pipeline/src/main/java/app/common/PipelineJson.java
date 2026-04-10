package app.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared Jackson configuration and helpers for pipeline metadata files (manifest, scene_analysis, etc.).
 */
public final class PipelineJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private PipelineJson() {}

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static void writeDocument(Path path, Object value) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        MAPPER.writeValue(path.toFile(), value);
    }
}
