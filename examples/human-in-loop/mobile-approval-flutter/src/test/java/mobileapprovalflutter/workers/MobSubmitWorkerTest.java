package mobileapprovalflutter.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MobSubmitWorkerTest {

    @Test
    void taskDefName() {
        MobSubmitWorker worker = new MobSubmitWorker();
        assertEquals("mob_submit", worker.getTaskDefName());
    }

    @Test
    void returnsSubmittedTrue() {
        MobSubmitWorker worker = new MobSubmitWorker();
        Task task = taskWith(new HashMap<>(Map.of("requestId", "REQ-001", "userId", "user-42")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("submitted"));
    }

    @Test
    void returnsSubmittedTrueWithEmptyInput() {
        MobSubmitWorker worker = new MobSubmitWorker();
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("submitted"));
    }

    @Test
    void outputContainsSubmittedKey() {
        MobSubmitWorker worker = new MobSubmitWorker();
        Task task = taskWith(new HashMap<>(Map.of("requestId", "REQ-999")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("submitted"));
    }

    @Test
    void alwaysCompletes() {
        MobSubmitWorker worker = new MobSubmitWorker();
        Task task1 = taskWith(new HashMap<>());
        Task task2 = taskWith(new HashMap<>(Map.of("requestId", "REQ-A")));

        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task1).getStatus());
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task2).getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
