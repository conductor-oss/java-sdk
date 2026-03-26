package hronboarding.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProvisionWorkerTest {

    private final ProvisionWorker worker = new ProvisionWorker();

    @Test
    void taskDefName() {
        assertEquals("hro_provision", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void provisionsSystems() {
        Task task = taskWith(Map.of("employeeId", "EMP-605", "department", "Engineering"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<String> systems = (List<String>) result.getOutputData().get("systems");
        assertTrue(systems.contains("email"));
        assertTrue(systems.contains("slack"));
        assertNotNull(result.getOutputData().get("laptop"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
