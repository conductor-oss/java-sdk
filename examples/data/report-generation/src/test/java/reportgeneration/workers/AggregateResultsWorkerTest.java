package reportgeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateResultsWorkerTest {

    private final AggregateResultsWorker worker = new AggregateResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("rg_aggregate_results", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void aggregatesRevenue() {
        List<Map<String, Object>> data = List.of(
                Map.of("region", "North", "product", "Widget", "revenue", 10000, "units", 100),
                Map.of("region", "South", "product", "Widget", "revenue", 5000, "units", 50));
        Task task = taskWith(Map.of("rawData", data));
        TaskResult result = worker.execute(task);

        Map<String, Object> aggregated = (Map<String, Object>) result.getOutputData().get("aggregated");
        assertNotNull(aggregated);
        assertEquals("$15,000", aggregated.get("totalRevenue"));
        assertEquals(150, aggregated.get("totalUnits"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void identifiesTopRegion() {
        List<Map<String, Object>> data = List.of(
                Map.of("region", "North", "product", "Widget", "revenue", 20000, "units", 100),
                Map.of("region", "South", "product", "Widget", "revenue", 5000, "units", 50));
        Task task = taskWith(Map.of("rawData", data));
        TaskResult result = worker.execute(task);

        Map<String, Object> aggregated = (Map<String, Object>) result.getOutputData().get("aggregated");
        assertEquals("North", aggregated.get("topRegion"));
    }

    @Test
    void returnsAggregationCount() {
        Task task = taskWith(Map.of("rawData", List.of(
                Map.of("region", "North", "product", "A", "revenue", 100, "units", 10))));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("aggregationCount"));
    }

    @Test
    void handlesEmptyData() {
        Task task = taskWith(Map.of("rawData", List.of()));
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
