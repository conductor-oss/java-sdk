package clinicaltrials.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class MonitorWorkerTest {
    private final MonitorWorker w = new MonitorWorker();

    @Test void taskDefName() { assertEquals("clt_monitor", w.getTaskDefName()); }

    @Test void monitorsParticipant() {
        TaskResult r = exec(Map.of("participantId", "P1", "group", "treatment"));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("monitoringData"));
        assertNotNull(r.getOutputData().get("cfr11AuditTrail"));
    }

    @Test void cfr11AuditTrailContainsRequiredFields() {
        TaskResult r = exec(Map.of("participantId", "P2", "group", "control"));
        Map<String, Object> cfr11 = (Map<String, Object>) r.getOutputData().get("cfr11AuditTrail");
        assertNotNull(cfr11.get("timestamp"));
        assertEquals("monitoring", cfr11.get("action"));
        assertNotNull(cfr11.get("performedBy"));
        assertNotNull(cfr11.get("electronicSignature"));
    }

    @Test void failsWithTerminalErrorOnMissingParticipantId() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("group", "treatment")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("participantId"));
    }

    @Test void failsWithTerminalErrorOnMissingGroup() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("participantId", "P1")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("group"));
    }

    @Test void failsWithTerminalErrorOnInvalidGroup() {
        TaskResult r = exec(Map.of("participantId", "P1", "group", "placebo"));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("Invalid group"));
    }

    private TaskResult exec(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(input));
        return w.execute(t);
    }
}
