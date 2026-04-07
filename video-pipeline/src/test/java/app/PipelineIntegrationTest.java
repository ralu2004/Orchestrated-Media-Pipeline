package app;

import app.common.FfmpegRunner;
import app.model.*;
import app.services.analysis.*;
import app.services.ingest.*;
import app.services.visuals.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PipelineIntegrationTest {

    private static final String JOB_ID = "test_job";

    private Path sampleVideoPath;
    private Path outputJobDir;

    private JobRequest jobRequest;
    private IngestResult ingestResult;

    @BeforeAll
    void setup() throws Exception {
        Path moduleDir = Paths.get("").toAbsolutePath(); // .../video-pipeline
        sampleVideoPath = moduleDir.resolve("../samples/input.mp4").normalize().toAbsolutePath();
        assertTrue(Files.exists(sampleVideoPath), "Sample video not found at " + sampleVideoPath);

        outputJobDir = moduleDir.resolve("output").resolve(JOB_ID).toAbsolutePath();
        deleteDirectoryIfExists(outputJobDir);

        jobRequest = new JobRequest(JOB_ID, sampleVideoPath.toString(), null);
    }

    @AfterAll
    void cleanup() throws Exception {
        deleteDirectoryIfExists(outputJobDir);
    }

    @Test @Order(1)
    void ingest_phase_works() throws Exception {
        FfmpegRunner runner = new FfmpegRunner("INGESTING");
        IntegrityCheckService integrity = new IntegrityCheckService();
        FormatValidatorService formatValidator = new FormatValidatorService(runner);
        DefaultIngestService ingest = new DefaultIngestService(integrity, formatValidator);

        ingestResult = ingest.process(jobRequest);

        assertNotNull(ingestResult.formatInfo());
        assertTrue(ingestResult.formatInfo().fileFormat().contains("mp4"));
        assertTrue(ingestResult.formatInfo().duration() > 0);
    }

    @Test @Order(2)
    void analysis_phase_works() throws Exception {
        if (ingestResult == null) ingest_phase_works();

        FfmpegRunner runner = new FfmpegRunner("ANALYZING");
        IntroOutroDetectorService introOutro = new IntroOutroDetectorService();
        CreditRollerService credits = new CreditRollerService();
        SceneIndexerService scenes = new SceneIndexerService();
        DefaultAnalysisService analysis = new DefaultAnalysisService(runner, introOutro, credits, scenes);

        AnalysisContext ctx = new AnalysisContext(jobRequest, ingestResult);
        AnalysisResult result = analysis.process(ctx);

        assertNotNull(result.introEnd());
        assertNotNull(result.outroStart());
        assertNotNull(result.creditsTimestamp());
        assertNotNull(result.segments());
        assertFalse(result.segments().isEmpty());

        Set<String> allowed = Set.of("dialogue", "action", "establishing_shot");
        for (SceneSegment seg : result.segments()) {
            assertNotNull(seg.category());
            assertTrue(allowed.contains(seg.category()), "Unexpected category: " + seg.category());
        }
    }

    @Test @Order(3)
    void visuals_phase_works() throws Exception {
        FfmpegRunner runner = new FfmpegRunner("PROCESSING");
        SceneComplexityService complexity = new SceneComplexityService(runner);
        TranscoderService transcoder = new TranscoderService(runner);
        SpriteGeneratorService sprites = new SpriteGeneratorService(runner);
        DefaultVisualsService visuals = new DefaultVisualsService(complexity, transcoder, sprites);

        VisualsResult result = visuals.process(jobRequest);

        assertNotNull(result.encodingProfile());
        assertTrue(result.encodingProfile().bitrate() > 0);

        Path h264Dir = outputJobDir.resolve("video").resolve("h264");
        Path vp9Dir = outputJobDir.resolve("video").resolve("vp9");
        Path hevcDir = outputJobDir.resolve("video").resolve("hevc");

        assertTrue(Files.isDirectory(h264Dir));
        assertEquals(3, countFiles(h264Dir));
        assertTrue(Files.isDirectory(vp9Dir));
        assertEquals(3, countFiles(vp9Dir));
        assertTrue(Files.isDirectory(hevcDir));
        assertEquals(3, countFiles(hevcDir));

        Path sprite = outputJobDir.resolve("images").resolve("sprite_map.jpg");
        assertTrue(Files.exists(sprite));
        assertTrue(Files.size(sprite) > 0);
    }

    private static int countFiles(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return (int) s.filter(Files::isRegularFile).count();
        }
    }

    private static void deleteDirectoryIfExists(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}

