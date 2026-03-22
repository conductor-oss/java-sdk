package clinicaltrials.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class ConsentWorkerTest {
    private final ConsentWorker w = new ConsentWorker();

    @Test void taskDefName() { assertEquals("clt_consent", w.getTaskDefName()); }

    @Test void recordsConsent() {
        TaskResult r = exec(Map.of("participantId", "P1", "trialId", "TRIAL-001"));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("consented"));
        assertNotNull(r.getOutputData().get("signatureHash"));
        assertNotNull(r.getOutputData().get("cfr11AuditTrail"));
    }

    @Test void cfr11AuditTrailContainsRequiredFields() {
        TaskResult r = exec(Map.of("participantId", "P2", "trialId", "TRIAL-002"));
        Map<String, Object> cfr11 = (Map<String, Object>) r.getOutputData().get("cfr11AuditTrail");
        assertNotNull(cfr11.get("timestamp"));
        assertEquals("informed_consent", cfr11.get("action"));
        assertNotNull(cfr11.get("performedBy"));
        assertNotNull(cfr11.get("electronicSignature"));
        assertEquals("P2", cfr11.get("participantId"));
        assertEquals("TRIAL-002", cfr11.get("trialId"));
    }

    @Test void failsWithTerminalErrorOnMissingParticipantId() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("trialId", "TRIAL-001")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("participantId"));
    }

    @Test void failsWithTerminalErrorOnMissingTrialId() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("participantId", "P1")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("trialId"));
    }

    private TaskResult exec(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(input));
        return w.execute(t);
    }
}
