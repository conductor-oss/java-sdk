package automatedtesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RunUnitTest {

    private final RunUnit worker = new RunUnit();

    @Test
    void taskDefName() {
        assertEquals("at_run_unit", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith("BLD-200001");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsPassed() {
        Task task = taskWith("BLD-200001");
        TaskResult result = worker.execute(task);
        assertEquals(247, result.getOutputData().get("passed"));
    }

    @Test
    void outputContainsFailed() {
        Task task = taskWith("BLD-200001");
        TaskResult result = worker.execute(task);
        assertEquals(0, result.getOutputData().get("failed"));
    }

    @Test
    void outputContainsDuration() {
        Task task = taskWith("BLD-200001");
        TaskResult result = worker.execute(task);
        assertEquals(12, result.getOutputData().get("duration"));
    }

    @Test
    void outputHasThreeFields() {
        Task task = taskWith("BLD-200001");
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("passed"));
        assertTrue(result.getOutputData().containsKey("failed"));
        assertTrue(result.getOutputData().containsKey("duration"));
    }

    @Test
    void deterministicOutput() {
        Task task = taskWith("BLD-200001");
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);
        assertEquals(r1.getOutputData(), r2.getOutputData());
    }

    private Task taskWith(String buildId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("buildId", buildId);
        task.setInputData(input);
        return task;
    }
}
