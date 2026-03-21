package securityincident.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TriageWorkerTest {

    private final TriageWorker worker = new TriageWorker();

    @Test
    void taskDefName() {
        assertEquals("si_triage", worker.getTaskDefName());
    }

    @Test
    void triagesUnauthorizedAccess() {
        Task task = taskWith(Map.of(
                "incidentType", "unauthorized-access",
                "severity", "P1",
                "affectedSystem", "api-gateway"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TRIAGE-1381", result.getOutputData().get("triageId"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void triagesMalwareDetected() {
        Task task = taskWith(Map.of(
                "incidentType", "malware-detected",
                "severity", "P2",
                "affectedSystem", "workstation-42"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TRIAGE-1381", result.getOutputData().get("triageId"));
    }

    @Test
    void triagesDataExfiltration() {
        Task task = taskWith(Map.of(
                "incidentType", "data-exfiltration",
                "severity", "P1",
                "affectedSystem", "database-server"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void handlesNullIncidentType() {
        Map<String, Object> input = new HashMap<>();
        input.put("incidentType", null);
        input.put("severity", "P2");
        input.put("affectedSystem", "web-server");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TRIAGE-1381", result.getOutputData().get("triageId"));
    }

    @Test
    void handlesNullSeverity() {
        Map<String, Object> input = new HashMap<>();
        input.put("incidentType", "phishing");
        input.put("severity", null);
        input.put("affectedSystem", "email-server");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TRIAGE-1381", result.getOutputData().get("triageId"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of(
                "incidentType", "brute-force",
                "severity", "P3",
                "affectedSystem", "auth-service"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("triageId"));
        assertTrue(result.getOutputData().containsKey("success"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
