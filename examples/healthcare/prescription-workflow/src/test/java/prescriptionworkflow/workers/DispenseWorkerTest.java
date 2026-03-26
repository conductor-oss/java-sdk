package prescriptionworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DispenseWorkerTest {

    private final DispenseWorker worker = new DispenseWorker();

    @Test
    void taskDefName() {
        assertEquals("prx_dispense", worker.getTaskDefName());
    }

    @Test
    void dispensesSuccessfully() {
        Task task = taskWith("FILL-78901", "PAT-001");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("dispensed"));
        assertNotNull(result.getOutputData().get("dispensedAt"));
        assertEquals("R. Patel, PharmD", result.getOutputData().get("pharmacist"));
    }

    @Test
    void handlesNullFillIdAndPatientId() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("dispensed"));
    }

    @Test
    void dispensedAtIsTimestamp() {
        Task task = taskWith("FILL-99999", "PAT-002");

        TaskResult result = worker.execute(task);

        String dispensedAt = (String) result.getOutputData().get("dispensedAt");
        assertNotNull(dispensedAt);
        assertTrue(dispensedAt.contains("T"), "dispensedAt should be ISO timestamp");
    }

    private Task taskWith(String fillId, String patientId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("fillId", fillId);
        input.put("patientId", patientId);
        task.setInputData(input);
        return task;
    }
}
