package waitforevent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrepareWorkerTest {

    private final PrepareWorker worker = new PrepareWorker();

    @Test
    void taskDefName() {
        assertEquals("we_prepare", worker.getTaskDefName());
    }

    @Test
    void preparesRequestSuccessfully() {
        Task task = taskWith(Map.of("requestId", "REQ-001", "requester", "alice"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("prepared"));
        assertEquals("REQ-001", result.getOutputData().get("requestId"));
        assertEquals("alice", result.getOutputData().get("requester"));
    }

    @Test
    void handlesNullRequestId() {
        Map<String, Object> input = new HashMap<>();
        input.put("requestId", null);
        input.put("requester", "bob");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("prepared"));
        assertNull(result.getOutputData().get("requestId"));
        assertEquals("bob", result.getOutputData().get("requester"));
    }

    @Test
    void handlesNullRequester() {
        Map<String, Object> input = new HashMap<>();
        input.put("requestId", "REQ-002");
        input.put("requester", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("prepared"));
        assertEquals("REQ-002", result.getOutputData().get("requestId"));
        assertNull(result.getOutputData().get("requester"));
    }

    @Test
    void outputContainsPreparedFlag() {
        Task task = taskWith(Map.of("requestId", "REQ-003", "requester", "carol"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("prepared"));
        assertEquals(true, result.getOutputData().get("prepared"));
    }

    @Test
    void preservesRequestIdInOutput() {
        Task task = taskWith(Map.of("requestId", "REQ-XYZ-999", "requester", "dave"));
        TaskResult result = worker.execute(task);

        assertEquals("REQ-XYZ-999", result.getOutputData().get("requestId"));
    }

    @Test
    void statusIsAlwaysCompleted() {
        Task task = taskWith(Map.of("requestId", "REQ-004", "requester", "eve"));
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
