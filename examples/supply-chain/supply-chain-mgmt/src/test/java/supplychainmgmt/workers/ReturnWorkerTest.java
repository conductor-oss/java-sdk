package supplychainmgmt.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReturnWorkerTest {

    private final ReturnWorker worker = new ReturnWorker();

    @Test
    void taskDefName() {
        assertEquals("scm_return", worker.getTaskDefName());
    }

    @Test
    void configuresReturnPolicy() {
        Task task = taskWith(Map.of("deliveryId", "DEL-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("30-day return window", result.getOutputData().get("returnPolicy"));
        assertEquals(true, result.getOutputData().get("active"));
    }

    @Test
    void handlesNullDeliveryId() {
        Map<String, Object> input = new HashMap<>();
        input.put("deliveryId", null);
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
