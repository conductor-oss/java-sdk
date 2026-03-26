package waittaskbasics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WaitAfterWorkerTest {

    @Test
    void taskDefName() {
        WaitAfterWorker worker = new WaitAfterWorker();
        assertEquals("wait_after", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedWithRequestId() {
        WaitAfterWorker worker = new WaitAfterWorker();
        Task task = taskWith(Map.of("requestId", "req-123", "approval", "approved"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("completed-req-123", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullRequestId() {
        WaitAfterWorker worker = new WaitAfterWorker();
        Task task = taskWith(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("completed-unknown", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullApproval() {
        WaitAfterWorker worker = new WaitAfterWorker();
        Map<String, Object> input = new HashMap<>();
        input.put("requestId", "req-456");
        Task task = taskWith(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("completed-req-456", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsResultKey() {
        WaitAfterWorker worker = new WaitAfterWorker();
        Task task = taskWith(Map.of("requestId", "req-789", "approval", "approved"));

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
    }

    @Test
    void handlesNumericRequestId() {
        WaitAfterWorker worker = new WaitAfterWorker();
        Task task = taskWith(Map.of("requestId", 42, "approval", "approved"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("completed-42", result.getOutputData().get("result"));
    }

    @Test
    void determinisiticOutputForSameInput() {
        WaitAfterWorker worker = new WaitAfterWorker();
        Task task1 = taskWith(Map.of("requestId", "req-same", "approval", "yes"));
        Task task2 = taskWith(Map.of("requestId", "req-same", "approval", "yes"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("result"), result2.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
