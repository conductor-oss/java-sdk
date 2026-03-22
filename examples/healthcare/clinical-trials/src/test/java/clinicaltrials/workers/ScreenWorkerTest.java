package clinicaltrials.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class ScreenWorkerTest {
    private final ScreenWorker w = new ScreenWorker();

    @Test void taskDefName() { assertEquals("clt_screen", w.getTaskDefName()); }

    @Test void eligiblePatient() {
        TaskResult r = exec(Map.of("participantId", "P1", "condition", "hypertension", "age", 35));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("eligible"));
        assertNotNull(r.getOutputData().get("cfr11AuditTrail"));
    }

    @Test void tooYoung() {
        TaskResult r = exec(Map.of("participantId", "P2", "condition", "hypertension", "age", 16));
        assertEquals(false, r.getOutputData().get("eligible"));
        assertEquals(false, r.getOutputData().get("ageEligible"));
    }

    @Test void tooOld() {
        TaskResult r = exec(Map.of("participantId", "P-OLD", "condition", "hypertension", "age", 70));
        assertEquals(false, r.getOutputData().get("eligible"));
        assertEquals(false, r.getOutputData().get("ageEligible"));
    }

    @Test void excludedCondition() {
        TaskResult r = exec(Map.of("participantId", "P3", "condition", "pregnancy", "age", 30));
        assertEquals(false, r.getOutputData().get("eligible"));
        assertEquals(true, r.getOutputData().get("hasExclusion"));
    }

    @Test void nonEligibleCondition() {
        TaskResult r = exec(Map.of("participantId", "P4", "condition", "common_cold", "age", 30));
        assertEquals(false, r.getOutputData().get("eligible"));
        assertEquals(false, r.getOutputData().get("conditionEligible"));
    }

    @Test void cfr11AuditTrailContainsRequiredFields() {
        TaskResult r = exec(Map.of("participantId", "P5", "condition", "hypertension", "age", 35));
        Map<String, Object> cfr11 = (Map<String, Object>) r.getOutputData().get("cfr11AuditTrail");
        assertNotNull(cfr11);
        assertNotNull(cfr11.get("timestamp"));
        assertEquals("screening", cfr11.get("action"));
        assertNotNull(cfr11.get("performedBy"));
        assertNotNull(cfr11.get("electronicSignature"));
        assertEquals("P5", cfr11.get("participantId"));
    }

    // ---- Failure path ---------------------------------------------------

    @Test void failsWithTerminalErrorOnMissingParticipantId() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("condition", "hypertension", "age", 35)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("participantId"));
    }

    @Test void failsWithTerminalErrorOnMissingCondition() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("participantId", "P1", "age", 35)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("condition"));
    }

    @Test void failsWithTerminalErrorOnMissingAge() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("participantId", "P1", "condition", "hypertension")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("age"));
    }

    @Test void failsWithTerminalErrorOnNegativeAge() {
        TaskResult r = exec(Map.of("participantId", "P-NEG", "condition", "hypertension", "age", -5));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("Invalid age"));
    }

    private TaskResult exec(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(input));
        return w.execute(t);
    }
}
