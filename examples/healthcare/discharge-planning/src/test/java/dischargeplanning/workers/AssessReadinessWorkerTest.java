package dischargeplanning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssessReadinessWorkerTest {

    private final AssessReadinessWorker worker = new AssessReadinessWorker();

    @Test void taskDefName() { assertEquals("dsc_assess_readiness", worker.getTaskDefName()); }

    @Test void returnsReadyStatus() {
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P1", "admissionId", "A1", "diagnosis", "AMI")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("ready", r.getOutputData().get("readiness"));
    }

    @Test void returnsNeeds() {
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P2", "admissionId", "A2", "diagnosis", "CHF")));
        @SuppressWarnings("unchecked") List<String> needs = (List<String>) r.getOutputData().get("needs");
        assertEquals(3, needs.size());
        assertTrue(needs.contains("home health aide"));
    }

    @Test void returnsLengthOfStay() {
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P3", "admissionId", "A3", "diagnosis", "Pneumonia")));
        assertEquals(4, r.getOutputData().get("lengthOfStay"));
    }

    @Test void handlesNullPatientId() {
        Map<String, Object> input = new HashMap<>(); input.put("patientId", null); input.put("admissionId", "A4"); input.put("diagnosis", "Flu");
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(input)).getStatus());
    }

    @Test void handlesMissingInputs() {
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(Map.of())).getStatus());
    }

    @Test void needsContainsMedReconciliation() {
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P5", "admissionId", "A5", "diagnosis", "Stroke")));
        @SuppressWarnings("unchecked") List<String> needs = (List<String>) r.getOutputData().get("needs");
        assertTrue(needs.contains("medication reconciliation"));
    }

    @Test void outputContainsAllKeys() {
        TaskResult r = worker.execute(taskWith(Map.of("patientId", "P6", "admissionId", "A6", "diagnosis", "DVT")));
        assertTrue(r.getOutputData().containsKey("readiness"));
        assertTrue(r.getOutputData().containsKey("needs"));
        assertTrue(r.getOutputData().containsKey("lengthOfStay"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
