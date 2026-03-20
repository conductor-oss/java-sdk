package autoscaling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PlanTest {

    private final Plan worker = new Plan();

    @Test
    void taskDefName() {
        assertEquals("as_plan", worker.getTaskDefName());
    }

    @Test
    void highLoadTriggersScaleUp() {
        Task task = taskWith(85);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("scale-up", result.getOutputData().get("action"));
        assertEquals(5, result.getOutputData().get("to"));
    }

    @Test
    void lowLoadTriggersScaleDown() {
        Task task = taskWith(25);
        TaskResult result = worker.execute(task);

        assertEquals("scale-down", result.getOutputData().get("action"));
        assertEquals(2, result.getOutputData().get("to"));
    }

    @Test
    void moderateLoadNoChange() {
        Task task = taskWith(50);
        TaskResult result = worker.execute(task);

        assertEquals("no-change", result.getOutputData().get("action"));
        assertEquals(3, result.getOutputData().get("to"));
    }

    @Test
    void boundaryAt80TriggersScaleUp() {
        Task task = taskWith(80);
        TaskResult result = worker.execute(task);

        assertEquals("scale-up", result.getOutputData().get("action"));
    }

    @Test
    void boundaryAt30TriggersScaleDown() {
        Task task = taskWith(30);
        TaskResult result = worker.execute(task);

        assertEquals("scale-down", result.getOutputData().get("action"));
    }

    @Test
    void boundaryAt31NoChange() {
        Task task = taskWith(31);
        TaskResult result = worker.execute(task);

        assertEquals("no-change", result.getOutputData().get("action"));
    }

    @Test
    void outputContainsAllFields() {
        Task task = taskWith(85);
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("action"));
        assertNotNull(result.getOutputData().get("from"));
        assertNotNull(result.getOutputData().get("to"));
        assertNotNull(result.getOutputData().get("reason"));
        assertNotNull(result.getOutputData().get("currentLoad"));
    }

    @Test
    void scaleDownNeverGoesBelowOne() {
        // Even with very low load, from=3 -> to=max(1, 3-1)=2
        Task task = taskWith(10);
        TaskResult result = worker.execute(task);

        int to = (int) result.getOutputData().get("to");
        assertTrue(to >= 1);
    }

    @Test
    void nullLoadUsesDefault() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("no-change", result.getOutputData().get("action"));
    }

    private Task taskWith(int currentLoad) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("currentLoad", currentLoad);
        task.setInputData(input);
        return task;
    }
}
