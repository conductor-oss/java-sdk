package goaldecomposition.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateWorkerTest {

    private final AggregateWorker worker = new AggregateWorker();

    @Test
    void taskDefName() {
        assertEquals("gd_aggregate", worker.getTaskDefName());
    }

    @Test
    void aggregatesAllThreeResults() {
        Task task = taskWith(Map.of(
                "goal", "Improve application performance by 3x",
                "result1", "Identified 3 bottlenecks: database queries (40%), API serialization (25%), network latency (15%)",
                "result2", "Recommended Redis caching (70% hit rate expected), query optimization, and response compression",
                "result3", "Horizontal pod autoscaling + read replicas can handle 3x current load at 20% cost increase"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String aggregated = (String) result.getOutputData().get("aggregatedResult");
        assertNotNull(aggregated);
        assertTrue(aggregated.contains("bottlenecks"));
        assertTrue(aggregated.contains("Redis caching"));
        assertTrue(aggregated.contains("autoscaling"));
    }

    @Test
    void aggregatedResultContainsAllParts() {
        Task task = taskWith(Map.of(
                "goal", "Improve performance",
                "result1", "Part A",
                "result2", "Part B",
                "result3", "Part C"));
        TaskResult result = worker.execute(task);

        String aggregated = (String) result.getOutputData().get("aggregatedResult");
        assertEquals("Part A; Part B; Part C", aggregated);
    }

    @Test
    void concatenatesWithSemicolonSeparator() {
        Task task = taskWith(Map.of(
                "goal", "Test goal",
                "result1", "First",
                "result2", "Second",
                "result3", "Third"));
        TaskResult result = worker.execute(task);

        String aggregated = (String) result.getOutputData().get("aggregatedResult");
        assertTrue(aggregated.contains("; "));
        assertEquals("First; Second; Third", aggregated);
    }

    @Test
    void handlesEmptyGoal() {
        Map<String, Object> input = new HashMap<>();
        input.put("goal", "");
        input.put("result1", "R1");
        input.put("result2", "R2");
        input.put("result3", "R3");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("aggregatedResult"));
    }

    @Test
    void handlesNullGoal() {
        Map<String, Object> input = new HashMap<>();
        input.put("goal", null);
        input.put("result1", "R1");
        input.put("result2", "R2");
        input.put("result3", "R3");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("aggregatedResult"));
    }

    @Test
    void handlesNullResults() {
        Map<String, Object> input = new HashMap<>();
        input.put("goal", "Some goal");
        input.put("result1", null);
        input.put("result2", null);
        input.put("result3", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String aggregated = (String) result.getOutputData().get("aggregatedResult");
        assertNotNull(aggregated);
        assertEquals("; ; ", aggregated);
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("aggregatedResult"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
