package gracefuldegradation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CoreProcessWorkerTest {

    private final CoreProcessWorker worker = new CoreProcessWorker();

    @Test
    void taskDefName() {
        assertEquals("gd_core_process", worker.getTaskDefName());
    }

    @Test
    void processesProvidedData() {
        Task task = taskWith(Map.of("data", "order-123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed-order-123", result.getOutputData().get("result"));
    }

    @Test
    void usesDefaultWhenNoData() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed-default", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullDataValue() {
        Map<String, Object> input = new HashMap<>();
        input.put("data", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed-default", result.getOutputData().get("result"));
    }

    @Test
    void processesNumericData() {
        Task task = taskWith(Map.of("data", 42));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed-42", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsResultKey() {
        Task task = taskWith(Map.of("data", "test"));
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("result"));
    }

    @Test
    void sameInputProducesSameOutput() {
        Task task1 = taskWith(Map.of("data", "order-abc"));
        Task task2 = taskWith(Map.of("data", "order-abc"));

        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("result"), r2.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
