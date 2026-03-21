package autoscaling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteTest {

    private final Execute worker = new Execute();

    @Test
    void taskDefName() {
        assertEquals("as_execute", worker.getTaskDefName());
    }

    @Test
    void scaleUpExecutesSuccessfully() {
        Task task = taskWith("scale-up");
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("scaled"));
        assertEquals(5, result.getOutputData().get("newInstanceCount"));
    }

    @Test
    void scaleDownExecutesSuccessfully() {
        Task task = taskWith("scale-down");
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("scaled"));
        assertEquals(2, result.getOutputData().get("newInstanceCount"));
    }

    @Test
    void noChangeReportsNotScaled() {
        Task task = taskWith("no-change");
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("scaled"));
        assertEquals(3, result.getOutputData().get("newInstanceCount"));
    }

    @Test
    void outputContainsExecutedAtTimestamp() {
        Task task = taskWith("scale-up");
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("executedAt"));
        String ts = (String) result.getOutputData().get("executedAt");
        assertTrue(ts.contains("T"), "Should be ISO timestamp");
    }

    @Test
    void scaleUpMessageMentionsReplicas() {
        Task task = taskWith("scale-up");
        TaskResult result = worker.execute(task);

        String msg = (String) result.getOutputData().get("message");
        assertTrue(msg.contains("5 replicas"));
    }

    @Test
    void scaleDownMessageMentionsReplicas() {
        Task task = taskWith("scale-down");
        TaskResult result = worker.execute(task);

        String msg = (String) result.getOutputData().get("message");
        assertTrue(msg.contains("2 replicas"));
    }

    @Test
    void nullActionDefaultsToNoChange() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("scaled"));
        assertEquals("no-change", result.getOutputData().get("action"));
    }

    private Task taskWith(String action) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("action", action);
        task.setInputData(input);
        return task;
    }
}
