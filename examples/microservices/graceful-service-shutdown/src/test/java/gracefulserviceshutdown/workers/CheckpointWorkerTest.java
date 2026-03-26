package gracefulserviceshutdown.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckpointWorkerTest {

    private final CheckpointWorker worker = new CheckpointWorker();

    @Test
    void taskDefName() {
        assertEquals("gs_checkpoint", worker.getTaskDefName());
    }

    @Test
    void savesCheckpointSuccessfully() {
        Task task = taskWith(Map.of("instanceId", "pod-1", "pendingTasks", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("saved"));
    }

    @Test
    void outputContainsInstanceId() {
        Task task = taskWith(Map.of("instanceId", "pod-2", "pendingTasks", 0));
        TaskResult result = worker.execute(task);

        assertEquals("pod-2", result.getOutputData().get("instanceId"));
    }

    @Test
    void outputContainsPendingTasks() {
        Task task = taskWith(Map.of("instanceId", "pod-3", "pendingTasks", 5));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("pendingTasks"));
    }

    @Test
    void handlesNullInstanceId() {
        Map<String, Object> input = new HashMap<>();
        input.put("instanceId", null);
        input.put("pendingTasks", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown-instance", result.getOutputData().get("instanceId"));
    }

    @Test
    void handlesNullPendingTasks() {
        Map<String, Object> input = new HashMap<>();
        input.put("instanceId", "pod-4");
        input.put("pendingTasks", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("pendingTasks"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("saved"));
    }

    @Test
    void handlesNonZeroPendingTasks() {
        Task task = taskWith(Map.of("instanceId", "pod-5", "pendingTasks", 10));
        TaskResult result = worker.execute(task);

        assertEquals(10, result.getOutputData().get("pendingTasks"));
        assertEquals(true, result.getOutputData().get("saved"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
