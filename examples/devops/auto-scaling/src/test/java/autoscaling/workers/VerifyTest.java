package autoscaling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VerifyTest {

    private final Verify worker = new Verify();

    @Test
    void taskDefName() {
        assertEquals("as_verify", worker.getTaskDefName());
    }

    @Test
    void scaleUpVerifiesSuccessfully() {
        Task task = taskWith("scale-up");
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
        assertEquals(52, result.getOutputData().get("newLoad"));
    }

    @Test
    void scaleDownVerifiesSuccessfully() {
        Task task = taskWith("scale-down");
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("verified"));
        assertEquals(45, result.getOutputData().get("newLoad"));
    }

    @Test
    void noChangeVerifiesSuccessfully() {
        Task task = taskWith("no-change");
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("verified"));
        assertEquals(50, result.getOutputData().get("newLoad"));
    }

    @Test
    void scaleUpReducesLoad() {
        Task task = taskWith("scale-up");
        TaskResult result = worker.execute(task);

        int newLoad = (int) result.getOutputData().get("newLoad");
        assertTrue(newLoad < 80, "After scale-up, load should be below threshold");
    }

    @Test
    void outputContainsMessage() {
        Task task = taskWith("scale-up");
        TaskResult result = worker.execute(task);

        String msg = (String) result.getOutputData().get("message");
        assertNotNull(msg);
        assertTrue(msg.contains("52%"));
    }

    @Test
    void scaleDownMessageReportsStableLoad() {
        Task task = taskWith("scale-down");
        TaskResult result = worker.execute(task);

        String msg = (String) result.getOutputData().get("message");
        assertTrue(msg.contains("45%"));
    }

    @Test
    void outputContainsActionField() {
        Task task = taskWith("scale-up");
        TaskResult result = worker.execute(task);

        assertEquals("scale-up", result.getOutputData().get("action"));
    }

    @Test
    void nullActionDefaultsToNoChange() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
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
