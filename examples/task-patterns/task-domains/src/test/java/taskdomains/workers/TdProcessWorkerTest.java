package taskdomains.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TdProcessWorkerTest {

    @Test
    void taskDefName() {
        TdProcessWorker worker = new TdProcessWorker();
        assertEquals("td_process", worker.getTaskDefName());
    }

    @Test
    void defaultWorkerGroup() {
        TdProcessWorker worker = new TdProcessWorker();
        assertEquals("default-group", worker.getWorkerGroup());
    }

    @Test
    void customWorkerGroup() {
        TdProcessWorker worker = new TdProcessWorker("gpu-group");
        assertEquals("gpu-group", worker.getWorkerGroup());
    }

    @Test
    void processesDataWithTypeAndValue() {
        TdProcessWorker worker = new TdProcessWorker("gpu-group");
        Task task = taskWith(Map.of("data", Map.of("type", "matrix", "value", 42)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed", result.getOutputData().get("result"));
        assertEquals("gpu-group", result.getOutputData().get("worker"));
    }

    @Test
    void processesWithDefaultGroup() {
        TdProcessWorker worker = new TdProcessWorker();
        Task task = taskWith(Map.of("data", Map.of("type", "vector", "value", 99)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed", result.getOutputData().get("result"));
        assertEquals("default-group", result.getOutputData().get("worker"));
    }

    @Test
    void handlesNullData() {
        TdProcessWorker worker = new TdProcessWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed", result.getOutputData().get("result"));
        assertEquals("default-group", result.getOutputData().get("worker"));
    }

    @Test
    void handlesDataWithMissingType() {
        TdProcessWorker worker = new TdProcessWorker("cpu-group");
        Task task = taskWith(Map.of("data", Map.of("value", 10)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed", result.getOutputData().get("result"));
        assertEquals("cpu-group", result.getOutputData().get("worker"));
    }

    @Test
    void handlesDataWithMissingValue() {
        TdProcessWorker worker = new TdProcessWorker("gpu-group");
        Task task = taskWith(Map.of("data", Map.of("type", "scalar")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
