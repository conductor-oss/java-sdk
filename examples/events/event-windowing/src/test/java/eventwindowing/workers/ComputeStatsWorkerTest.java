package eventwindowing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComputeStatsWorkerTest {

    private final ComputeStatsWorker worker = new ComputeStatsWorker();

    @Test
    void taskDefName() {
        assertEquals("ew_compute_stats", worker.getTaskDefName());
    }

    @Test
    void computesStatsForWindow() {
        List<Map<String, Object>> windowEvents = List.of(
                Map.of("ts", 1000, "value", 10),
                Map.of("ts", 1500, "value", 25),
                Map.of("ts", 2000, "value", 15),
                Map.of("ts", 2500, "value", 30),
                Map.of("ts", 3000, "value", 20));
        Task task = taskWith(Map.of("windowEvents", windowEvents, "windowId", "win_fixed_001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("stats"));
    }

    @Test
    void statsCountIsFive() {
        Task task = taskWith(Map.of("windowEvents", List.of(), "windowId", "win_fixed_001"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) result.getOutputData().get("stats");
        assertEquals(5, stats.get("count"));
    }

    @Test
    void statsMinIsTen() {
        Task task = taskWith(Map.of("windowEvents", List.of(), "windowId", "win_fixed_001"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) result.getOutputData().get("stats");
        assertEquals(10, stats.get("min"));
    }

    @Test
    void statsMaxIsThirty() {
        Task task = taskWith(Map.of("windowEvents", List.of(), "windowId", "win_fixed_001"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) result.getOutputData().get("stats");
        assertEquals(30, stats.get("max"));
    }

    @Test
    void statsSumIsOneHundred() {
        Task task = taskWith(Map.of("windowEvents", List.of(), "windowId", "win_fixed_001"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) result.getOutputData().get("stats");
        assertEquals(100, stats.get("sum"));
    }

    @Test
    void statsAvgIsStringTwentyPointZeroZero() {
        Task task = taskWith(Map.of("windowEvents", List.of(), "windowId", "win_fixed_001"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) result.getOutputData().get("stats");
        assertEquals("20.00", stats.get("avg"));
        assertTrue(stats.get("avg") instanceof String);
    }

    @Test
    void returnsWindowId() {
        Task task = taskWith(Map.of("windowEvents", List.of(), "windowId", "win_fixed_001"));
        TaskResult result = worker.execute(task);

        assertEquals("win_fixed_001", result.getOutputData().get("windowId"));
    }

    @Test
    void handlesNullWindowId() {
        Map<String, Object> input = new HashMap<>();
        input.put("windowEvents", List.of());
        input.put("windowId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("win_fixed_001", result.getOutputData().get("windowId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("stats"));
        assertEquals("win_fixed_001", result.getOutputData().get("windowId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
