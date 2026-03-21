package medicalrecordsreview.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MrValidateHipaaWorkerTest {

    @Test
    void taskDefName() {
        MrValidateHipaaWorker worker = new MrValidateHipaaWorker();
        assertEquals("mr_validate_hipaa", worker.getTaskDefName());
    }

    @Test
    void returnsCompliantTrue() {
        MrValidateHipaaWorker worker = new MrValidateHipaaWorker();
        Task task = taskWith(Map.of("recordId", "REC-001"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("compliant"));
    }

    @Test
    void returnsChecksMap() {
        MrValidateHipaaWorker worker = new MrValidateHipaaWorker();
        Task task = taskWith(Map.of("recordId", "REC-001"));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> checks = (Map<String, Object>) result.getOutputData().get("checks");
        assertNotNull(checks);
        assertEquals(true, checks.get("phiEncrypted"));
        assertEquals(true, checks.get("accessLogged"));
        assertEquals(true, checks.get("minimumNecessary"));
    }

    @Test
    void checksMapContainsAllThreeKeys() {
        MrValidateHipaaWorker worker = new MrValidateHipaaWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> checks = (Map<String, Object>) result.getOutputData().get("checks");
        assertNotNull(checks);
        assertEquals(3, checks.size());
        assertTrue(checks.containsKey("phiEncrypted"));
        assertTrue(checks.containsKey("accessLogged"));
        assertTrue(checks.containsKey("minimumNecessary"));
    }

    @Test
    void outputContainsCompliantAndChecksKeys() {
        MrValidateHipaaWorker worker = new MrValidateHipaaWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("compliant"));
        assertTrue(result.getOutputData().containsKey("checks"));
    }

    @Test
    void alwaysCompletes() {
        MrValidateHipaaWorker worker = new MrValidateHipaaWorker();
        Task task = taskWith(Map.of("extra", "data"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("compliant"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
