package app;

import app.common.FfmpegRunner;
import app.common.PipelineStageName;
import app.model.*;
import app.services.analysis.*;
import app.services.audio.*;
import app.services.compliance.*;
import app.services.ingest.*;
import app.services.packaging.DefaultPackagingService;
import app.services.packaging.DrmWrapper;
import app.services.packaging.ManifestBuilder;
import app.services.visuals.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PipelineIntegrationTest {

    private static final String JOB_ID = "test_job";

    private Path sampleVideoPath;
    private Path outputJobDir;

    private JobRequest jobRequest;
    private IngestResult ingestResult;
    private AnalysisResult analysisResult;
    private VisualsResult visualsResult;
    private AudioResult audioResult;
    private ComplianceResult complianceResult;

    private ExecutorService pipelineExecutor;

    @BeforeAll
    void setup() throws Exception {
        Path moduleDir = Paths.get("").toAbsolutePath(); // .../video-pipeline
        sampleVideoPath = moduleDir.resolve("../samples/input.mp4").normalize().toAbsolutePath();
        assertTrue(Files.exists(sampleVideoPath), "Sample video not found at " + sampleVideoPath);

        outputJobDir = moduleDir.resolve("output").resolve(JOB_ID).toAbsolutePath();
        deleteDirectoryIfExists(outputJobDir);

        jobRequest = new JobRequest(JOB_ID, sampleVideoPath.toString(), null);

        pipelineExecutor = Executors.newFixedThreadPool(2);
    }

    @AfterAll
    void cleanup() throws Exception {
        if (pipelineExecutor != null) {
            pipelineExecutor.shutdown();
        }
        deleteDirectoryIfExists(outputJobDir);
    }

    @Test @Order(1)
    void ingest_phase_works() throws Exception {
        FfmpegRunner runner = new FfmpegRunner(PipelineStageName.INGESTING);
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

        FfmpegRunner runner = new FfmpegRunner(PipelineStageName.ANALYZING);
        IntroOutroDetectorService introOutro = new IntroOutroDetectorService();
        CreditRollerService credits = new CreditRollerService();
        SceneIndexerService scenes = new SceneIndexerService();
        DefaultAnalysisService analysis = new DefaultAnalysisService(
                pipelineExecutor, runner, introOutro, credits, scenes);

        AnalysisContext ctx = new AnalysisContext(jobRequest, ingestResult);
        analysisResult = analysis.process(ctx);

        assertNotNull(analysisResult.introEnd());
        assertNotNull(analysisResult.outroStart());
        assertNotNull(analysisResult.creditsTimestamp());
        assertNotNull(analysisResult.segments());
        assertFalse(analysisResult.segments().isEmpty());

        Set<String> allowed = Set.of("dialogue", "action", "establishing_shot");
        for (SceneSegment seg : analysisResult.segments()) {
            assertNotNull(seg.category());
            assertTrue(allowed.contains(seg.category()), "Unexpected category: " + seg.category());
        }

        Path sceneAnalysis = outputJobDir.resolve("metadata").resolve("scene_analysis.json");
        assertTrue(Files.exists(sceneAnalysis));
        assertTrue(Files.size(sceneAnalysis) > 0);
    }

    @Test @Order(3)
    void visuals_phase_works() throws Exception {
        assumeVisualsPipelineTestsEnabled();

        FfmpegRunner runner = new FfmpegRunner(PipelineStageName.VISUALS);
        SceneComplexityService complexity = new SceneComplexityService(runner);
        TranscoderService transcoder = new TranscoderService(runner);
        SpriteGeneratorService sprites = new SpriteGeneratorService(runner);
        DefaultVisualsService visuals = new DefaultVisualsService(complexity, transcoder, sprites);

        visualsResult = visuals.process(jobRequest);

        assertNotNull(visualsResult.encodingProfile());
        assertTrue(visualsResult.encodingProfile().bitrate() > 0);

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

        assertNotNull(visualsResult.thumbnailPaths());
        assertFalse(visualsResult.thumbnailPaths().isEmpty());
        Path thumbsDir = outputJobDir.resolve("images").resolve("thumbnails");
        assertTrue(Files.isDirectory(thumbsDir));
        for (String p : visualsResult.thumbnailPaths()) {
            assertTrue(Files.exists(Path.of(p)), "Missing thumbnail: " + p);
        }
    }

    @Test @Order(4)
    void audio_phase_works() throws Exception {
        if (ingestResult == null) ingest_phase_works();
        if (analysisResult == null) analysis_phase_works();

        assumeTrue(canRunPython(), "Python is not available");
        Path moduleDir = Paths.get("").toAbsolutePath();
        assumeTrue(Files.exists(moduleDir.resolve("scripts/transcribe.py")), "transcribe.py is missing");
        assumeTrue(Files.exists(moduleDir.resolve("scripts/translate.py")), "translate.py is missing");
        assumeTrue(Files.exists(moduleDir.resolve("scripts/dub.py")), "dub.py is missing");

        DefaultAudioService audio = new DefaultAudioService(
                new SpeechToTextService(new FfmpegRunner(PipelineStageName.PROCESSING)),
                new TranslationService(),
                new AiDubberService());

        try {
            audioResult = audio.process(new AudioContext(jobRequest, analysisResult));
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            assumeTrue(
                    !(msg.contains("connection") || msg.contains("network") || msg.contains("timed out")),
                    "Skipping due to transient network issue: " + e.getMessage()
            );
            throw e;
        }

        assertNotNull(audioResult);
        assertNotNull(audioResult.transcriptPath());
        assertTrue(Files.exists(Path.of(audioResult.transcriptPath())));
        assertTrue(Files.size(Path.of(audioResult.transcriptPath())) > 0);
        assertTrue(audioResult.translations().containsKey("ro"));
        assertTrue(audioResult.syntheticAudio().containsKey("ro"));
        assertTrue(Files.exists(Path.of(audioResult.translations().get("ro"))));
        assertTrue(Files.exists(Path.of(audioResult.syntheticAudio().get("ro"))));
    }

    @Test @Order(5)
    void compliance_phase_works() throws Exception {
        assumeVisualsPipelineTestsEnabled();

        if (analysisResult == null) analysis_phase_works();
        if (visualsResult == null) visuals_phase_works();

        FfmpegRunner complianceRunner = new FfmpegRunner(PipelineStageName.COMPLIANCE);
        DefaultComplianceService complianceService = new DefaultComplianceService(
                new SafetyScannerService(complianceRunner),
                new RegionalBrandingService(complianceRunner));
        complianceResult = complianceService.process(new ComplianceContext(jobRequest, visualsResult));

        assertNotNull(complianceResult);
        assertNotNull(complianceResult.flags());
        assertFalse(complianceResult.flags().isEmpty());
        assertNotNull(complianceResult.processedVideos());
        assertEquals(visualsResult.transcodedVideos().size(), complianceResult.processedVideos().size());
        for (var v : complianceResult.processedVideos()) {
            String p = v.path().replace('\\', '/');
            assertTrue(p.contains("/compliance/video/"), "Expected branded output under compliance/video/: " + v.path());
            assertTrue(Files.exists(Path.of(v.path())));
        }
    }

    @Test @Order(6)
    void packaging_phase_works() throws Exception {
        assumeVisualsPipelineTestsEnabled();

        if (analysisResult == null) analysis_phase_works();
        if (visualsResult == null) visuals_phase_works();
        if (audioResult == null) audio_phase_works();
        if (complianceResult == null) compliance_phase_works();

        DefaultPackagingService packaging = new DefaultPackagingService(
                new DrmWrapper(new FfmpegRunner(PipelineStageName.PACKAGING)),
                new ManifestBuilder());
        PackagingResult result = packaging.process(
                new PackagingContext(jobRequest, visualsResult, audioResult, complianceResult));

        assertNotNull(result);
        assertNotNull(result.manifestPath());
        assertTrue(Files.exists(Path.of(result.manifestPath())));
        assertTrue(Files.exists(outputJobDir.resolve("metadata").resolve("scene_analysis.json")));
        String manifestContent = Files.readString(Path.of(result.manifestPath()));
        assertTrue(manifestContent.contains("sceneAnalysisPath"));
        assertTrue(manifestContent.contains("scene_analysis.json"));
        assertTrue(manifestContent.contains("thumbnails"));
        assertNotNull(result.encryptedAssets());
        assertEquals(complianceResult.processedVideos().size(), result.encryptedAssets().size());
        assertTrue(manifestContent.contains("drmAssets"));
        for (String drmPath : result.encryptedAssets()) {
            assertTrue(Files.exists(Path.of(drmPath)), "Simulated DRM file should exist: " + drmPath);
            assertTrue(Files.size(Path.of(drmPath)) > 0);
        }
        for (var v : complianceResult.processedVideos()) {
            assertTrue(manifestContent.contains(v.path().replace('\\', '/')));
        }
    }

    /**
     * Heavy ffmpeg transcoding / compliance / packaging integration steps are opt-in so {@code mvn test}
     * stays fast by default. Pass {@code -Drun.visuals.test=true} to run them.
     */
    private static void assumeVisualsPipelineTestsEnabled() {
        assumeTrue(
                Boolean.parseBoolean(System.getProperty("run.visuals.test", "false")),
                "Slow visuals pipeline tests skipped; run with -Drun.visuals.test=true");
    }

    private static int countFiles(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return (int) s.filter(Files::isRegularFile).count();
        }
    }

    private static boolean canRunPython() {
        try {
            Process process = new ProcessBuilder("python", "--version").start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
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

