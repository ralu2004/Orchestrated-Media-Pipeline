package app;

import app.model.JobRequest;
import app.model.JobStatus;
import app.orchestrator.Orchestrator;
import app.orchestrator.PipelineJob;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -jar video-pipeline.jar <jobId> <sourceFile> [expectedChecksum]");
            return;
        }

        String jobId = args[0];
        String sourceFile = args[1];
        String expectedChecksum = args.length >= 3 ? args[2] : null;

        JobRequest request = new JobRequest(jobId, sourceFile, expectedChecksum);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Orchestrator orchestrator = PipelineFactory.createOrchestrator(executor);

        try {
            PipelineJob job = orchestrator.run(request);

            System.out.println("Job status: " + job.getStatus());
            if (job.getStatus() == JobStatus.COMPLETED) {
                System.out.println("Manifest: " + job.getPackagingResult().manifestPath());
            } else if (job.getStatus() == JobStatus.FAILED && job.getFailureCause() != null) {
                System.out.println("Failed at stage: " + job.getFailureCause().getStageName());
                System.out.println("Reason: " + job.getFailureCause().getMessage());
            }
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
    }
}
