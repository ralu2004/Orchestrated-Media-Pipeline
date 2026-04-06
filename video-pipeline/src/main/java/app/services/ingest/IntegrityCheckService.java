package app.services.ingest;

import app.common.PipelineException;
import app.common.PipelineStage;
import app.model.JobRequest;

import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * Computes the SHA-256 checksum of the source file and validates it
 * against the expected checksum provided in the JobRequest.
 *
 * If checksums don't match, the pipeline is aborted — a corrupted or
 * tampered file should never proceed to processing.
 */
public class IntegrityCheckService implements PipelineStage<JobRequest, String> {

    @Override
    public String process(JobRequest input) throws PipelineException {
        try {
            String actualChecksum = computeChecksum(input.sourceFile());
            validateChecksum(actualChecksum, input.expectedChecksum());
            return actualChecksum;
        } catch (PipelineException e) {
            throw e;
        } catch (Exception e) {
            throw new PipelineException("Integrity check failed", "INGESTING", e);
        }
    }

    private String computeChecksum(String filePath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[8192]; //8kb buffer
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead); //buffer, index, no of bytes to read
            }
        }
        return bytesToHex(digest.digest());
    }

    private void validateChecksum(String actual, String expected) throws PipelineException {
        if (expected == null || expected.isBlank()) return; // no checksum provided, skip validation
        if (!actual.equalsIgnoreCase(expected)) {
            throw new PipelineException("Checksum mismatch — expected: " + expected + ", actual: " + actual, "INGESTING");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}