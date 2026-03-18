package patientintake.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RegisterWorkerTest {
    private final RegisterWorker worker = new RegisterWorker();

    @Test void taskDefName() { assertEquals("pit_register", worker.getTaskDefName()); }

    @Test void registersPatient() {
        Task t = taskWith(Map.of("patientId", "PAT-001", "name", "Sarah Johnson"));
        TaskResult result = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("MRN-PAT-001", result.getOutputData().get("mrn"));
    }

    @Test @SuppressWarnings("unchecked")
    void vitalsInNormalRange() {
        TaskResult result = worker.execute(taskWith(Map.of("patientId", "P1", "name", "Test")));
        Map<String, Object> vitals = (Map<String, Object>) result.getOutputData().get("vitals");
        assertNotNull(vitals.get("bp"));
        assertTrue(((Number) vitals.get("hr")).intValue() >= 60);
        assertTrue(((Number) vitals.get("spo2")).intValue() >= 95);
    }

    @Test void handlesNullPatientId() {
        Map<String, Object> input = new HashMap<>(); input.put("patientId", null); input.put("name", "Test");
        TaskResult result = worker.execute(taskWith(input));
        assertEquals("MRN-UNKNOWN", result.getOutputData().get("mrn"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
