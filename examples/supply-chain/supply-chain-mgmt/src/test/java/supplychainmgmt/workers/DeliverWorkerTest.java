package supplychainmgmt.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeliverWorkerTest {

    private final DeliverWorker worker = new DeliverWorker();

    @Test
    void taskDefName() {
        assertEquals("scm_deliver", worker.getTaskDefName());
    }

    @Test
    void deliversBatch() {
        Task task = taskWith(Map.of("batchId", "BATCH-001", "destination", "Chicago"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("DEL-651-001", result.getOutputData().get("deliveryId"));
        assertEquals("3 days", result.getOutputData().get("eta"));
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("batchId", null);
        input.put("destination", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
