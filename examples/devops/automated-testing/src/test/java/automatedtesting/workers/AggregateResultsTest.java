package automatedtesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AggregateResultsTest {

    private final AggregateResults worker = new AggregateResults();

    @Test
    void taskDefName() {
        assertEquals("at_aggregate_results", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith();
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void totalPassedIs277() {
        Task task = taskWith();
        TaskResult result = worker.execute(task);
        assertEquals(277, result.getOutputData().get("totalPassed"));
    }

    @Test
    void totalFailedIsZero() {
        Task task = taskWith();
        TaskResult result = worker.execute(task);
        assertEquals(0, result.getOutputData().get("totalFailed"));
    }

    @Test
    void coverageIs87() {
        Task task = taskWith();
        TaskResult result = worker.execute(task);
        assertEquals(87, result.getOutputData().get("coverage"));
    }

    @Test
    void outputHasThreeFields() {
        Task task = taskWith();
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("totalPassed"));
        assertTrue(result.getOutputData().containsKey("totalFailed"));
        assertTrue(result.getOutputData().containsKey("coverage"));
    }

    @Test
    void deterministicOutput() {
        Task task = taskWith();
        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);
        assertEquals(r1.getOutputData(), r2.getOutputData());
    }

    private Task taskWith() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("unit", Map.of("passed", 247, "failed", 0));
        input.put("integration", Map.of("passed", 18, "failed", 0));
        input.put("e2e", Map.of("passed", 12, "failed", 0));
        task.setInputData(input);
        return task;
    }
}
