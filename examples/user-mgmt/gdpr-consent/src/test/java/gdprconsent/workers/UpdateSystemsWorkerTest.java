package gdprconsent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdateSystemsWorkerTest {

    private final UpdateSystemsWorker worker = new UpdateSystemsWorker();

    @Test
    void taskDefName() {
        assertEquals("gdc_update_systems", worker.getTaskDefName());
    }

    @Test
    void updatesFourSystems() {
        Task task = taskWith(Map.of("userId", "USR-123", "consents", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4, result.getOutputData().get("systemsUpdated"));
    }

    @Test
    void returnsSystemList() {
        Task task = taskWith(Map.of("userId", "USR-123", "consents", Map.of()));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("systems"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
