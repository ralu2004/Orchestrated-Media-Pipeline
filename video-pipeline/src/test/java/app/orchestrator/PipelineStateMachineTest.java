package app.orchestrator;

import app.common.PipelineException;
import app.model.JobRequest;
import app.model.JobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class PipelineStateMachineTest {

    @Test
    void pipelinePathTransitionsAllowedByTable() {
        assertTrue(Transitions.isAllowed(JobStatus.PENDING, JobStatus.INGESTING));
        assertTrue(Transitions.isAllowed(JobStatus.INGESTING, JobStatus.ANALYZING));
        assertTrue(Transitions.isAllowed(JobStatus.ANALYZING, JobStatus.PROCESSING));
        assertTrue(Transitions.isAllowed(JobStatus.PROCESSING, JobStatus.COMPLIANCE));
        assertTrue(Transitions.isAllowed(JobStatus.COMPLIANCE, JobStatus.PACKAGING));
        assertTrue(Transitions.isAllowed(JobStatus.PACKAGING, JobStatus.COMPLETED));
    }

    @ParameterizedTest
    @EnumSource(
            value = JobStatus.class,
            names = {"PENDING", "INGESTING", "ANALYZING", "PROCESSING", "COMPLIANCE", "PACKAGING"})
    void failedIsAllowedFromAnyNonTerminalState(JobStatus from) {
        assertTrue(Transitions.isAllowed(from, JobStatus.FAILED));
    }

    @Test
    void failedIsNotAllowedFromCompletedOrAlreadyFailed() {
        assertFalse(Transitions.isAllowed(JobStatus.COMPLETED, JobStatus.FAILED));
        assertFalse(Transitions.isAllowed(JobStatus.FAILED, JobStatus.FAILED));
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, ANALYZING",
            "PENDING, COMPLETED",
            "INGESTING, PROCESSING",
            "ANALYZING, COMPLIANCE",
            "PROCESSING, PACKAGING",
            "COMPLIANCE, COMPLETED",
            "PACKAGING, INGESTING",
            "COMPLETED, INGESTING"
    })
    void skipsAndBackwardTransitionsAreRejected(String fromName, String toName) {
        JobStatus from = JobStatus.valueOf(fromName);
        JobStatus to = JobStatus.valueOf(toName);
        assertFalse(Transitions.isAllowed(from, to), () -> from + " -> " + to + " should be illegal");
    }

    @Test
    void pipelineJobRunsHappyPathInOrder() throws PipelineException {
        PipelineJob job = new PipelineJob(new JobRequest("sm-test", "/x", null));
        assertEquals(JobStatus.PENDING, job.getStatus());

        job.applyTransition(JobStatus.INGESTING);
        assertEquals(JobStatus.INGESTING, job.getStatus());
        job.applyTransition(JobStatus.ANALYZING);
        job.applyTransition(JobStatus.PROCESSING);
        job.applyTransition(JobStatus.COMPLIANCE);
        job.applyTransition(JobStatus.PACKAGING);
        job.applyTransition(JobStatus.COMPLETED);
        assertEquals(JobStatus.COMPLETED, job.getStatus());
    }

    @Test
    void pipelineJobRejectsIllegalTransition() throws PipelineException {
        PipelineJob job = new PipelineJob(new JobRequest("sm-test", "/x", null));
        job.applyTransition(JobStatus.INGESTING);

        PipelineException ex = assertThrows(PipelineException.class, () -> job.applyTransition(JobStatus.COMPLETED));
        assertEquals("ORCHESTRATION", ex.getStageName());
        assertTrue(ex.getMessage().contains("Illegal pipeline transition"));
        assertEquals(JobStatus.INGESTING, job.getStatus());
    }

    @Test
    void pipelineJobCanEnterFailedAfterPartialProgress() throws PipelineException {
        PipelineJob job = new PipelineJob(new JobRequest("sm-test", "/x", null));
        job.applyTransition(JobStatus.INGESTING);
        job.applyTransition(JobStatus.ANALYZING);
        job.applyTransition(JobStatus.FAILED);
        assertEquals(JobStatus.FAILED, job.getStatus());
    }
}
