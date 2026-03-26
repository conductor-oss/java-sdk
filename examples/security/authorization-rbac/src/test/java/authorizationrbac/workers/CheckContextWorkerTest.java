package authorizationrbac.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckContextWorkerTest {

    private final CheckContextWorker worker = new CheckContextWorker();

    @Test
    void taskDefName() {
        assertEquals("rbac_check_context", worker.getTaskDefName());
    }

    @Test
    void checksContextSuccessfully() {
        Task task = taskWith(Map.of("check_contextData", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("check_context"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsCheckContext() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("check_context"));
    }

    @Test
    void outputContainsProcessed() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesExtraInputs() {
        Task task = taskWith(Map.of("extra", "value"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void deterministicOutput() {
        Task t1 = taskWith(Map.of());
        Task t2 = taskWith(Map.of());
        assertEquals(worker.execute(t1).getOutputData(), worker.execute(t2).getOutputData());
    }

    @Test
    void statusIsCompleted() {
        Task task = taskWith(Map.of());
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task).getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
