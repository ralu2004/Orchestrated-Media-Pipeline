package app.services.analysis;

import app.common.PipelineException;
import app.model.AnalysisResult;
import app.model.JobRequest;
import app.model.SceneSegment;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DefaultAnalysisService implements AnalysisService {

    private final IntroOutroDetectorService introOutroDetector;
    private final CreditRollerService creditRoller;
    private final SceneIndexerService sceneIndexer;

    public DefaultAnalysisService(IntroOutroDetectorService introOutroDetector,
                                  CreditRollerService creditRoller,
                                  SceneIndexerService sceneIndexer) {
        this.introOutroDetector = introOutroDetector;
        this.creditRoller = creditRoller;
        this.sceneIndexer = sceneIndexer;
    }

    @Override
    public AnalysisResult process(JobRequest input) throws PipelineException {
        // run IntroOutro and CreditRoller in parallel
        CompletableFuture<String> introFuture = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return introOutroDetector.process(input);
                    } catch (PipelineException e) {
                        throw new RuntimeException(e);
                    }
                });

        CompletableFuture<String> creditsFuture = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return creditRoller.process(input);
                    } catch (PipelineException e) {
                        throw new RuntimeException(e);
                    }
                });

        try {
            CompletableFuture.allOf(introFuture, creditsFuture).join();
            String introTimestamp = introFuture.get();
            String creditsTimestamp = creditsFuture.get();

            AnalysisResult partialResult = new AnalysisResult(
                    introTimestamp, null, creditsTimestamp, null);

            List<SceneSegment> segments = sceneIndexer.process(partialResult);

            return new AnalysisResult(introTimestamp, "stub-outro", creditsTimestamp, segments);

        } catch (ExecutionException | InterruptedException e) {
            throw new PipelineException("Analysis phase failed", "ANALYZING", e);
        }
    }
}