package gracefulserviceshutdown.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DrainTasksWorkerTest {

    private final DrainTasksWorker worker = new DrainTasksWorker();

    @Test
    void taskDefName() {
        assertEquals("gs_drain_tasks", worker.getTaskDefName());
    }

    @Test
    void drainsTasksSuccessfully() {
        Task task = taskWith(Map.of("instanceId", "pod-1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("drained"));
    }

    @Test
    void pendingTasksIsZeroAfterDrain() {
        Task task = taskWith(Map.of("instanceId", "pod-2"));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("pendingTasks"));
    }

    @Test
    void outputContainsInstanceId() {
        Task task = taskWith(Map.of("instanceId", "pod-3"));
        TaskResult result = worker.execute(task);

        assertEquals("pod-3", result.getOutputData().get("instanceId"));
    }

    @Test
    void handlesNullInstanceId() {
        Map<String, Object> input = new HashMap<>();
        input.put("instanceId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown-instance", result.getOutputData().get("instanceId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("drained"));
    }

    @Test
    void handlesVariousInstanceIds() {
        Task task = taskWith(Map.of("instanceId", "order-svc-pod-99"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order-svc-pod-99", result.getOutputData().get("instanceId"));
    }

    @Test
    void drainedAlwaysTrue() {
        Task task = taskWith(Map.of("instanceId", "any-pod"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("drained"));
        assertEquals(0, result.getOutputData().get("pendingTasks"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
