package priceoptimization.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeDemandWorkerTest {

    private final AnalyzeDemandWorker worker = new AnalyzeDemandWorker();

    @Test
    void taskDefName() {
        assertEquals("prz_analyze_demand", worker.getTaskDefName());
    }

    @Test
    void returnsDemandScore() {
        Task task = taskWith(Map.of("productId", "P-1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.78, result.getOutputData().get("demandScore"));
    }

    @Test
    void returnsElasticity() {
        Task task = taskWith(Map.of("productId", "P-2"));
        TaskResult result = worker.execute(task);

        assertEquals(-1.2, result.getOutputData().get("elasticity"));
    }

    @Test
    void returnsSeasonalFactor() {
        Task task = taskWith(Map.of("productId", "P-3"));
        TaskResult result = worker.execute(task);

        assertEquals(1.15, result.getOutputData().get("seasonalFactor"));
    }

    @Test
    void returnsForecastedDemand() {
        Task task = taskWith(Map.of("productId", "P-4"));
        TaskResult result = worker.execute(task);

        assertEquals(340, result.getOutputData().get("forecastedDemand"));
    }

    @Test
    void handlesNullProductId() {
        Map<String, Object> input = new HashMap<>();
        input.put("productId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("demandScore"));
    }

    @Test
    void allOutputFieldsPresent() {
        Task task = taskWith(Map.of("productId", "P-5"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("demandScore"));
        assertTrue(result.getOutputData().containsKey("elasticity"));
        assertTrue(result.getOutputData().containsKey("seasonalFactor"));
        assertTrue(result.getOutputData().containsKey("forecastedDemand"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
