package energymanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OptimizeWorkerTest {

    private final OptimizeWorker worker = new OptimizeWorker();

    @Test
    void taskDefName() {
        assertEquals("erg_optimize", worker.getTaskDefName());
    }

    @Test
    void returnsProjectedSavings() {
        Task task = taskWith(Map.of("buildingId", "BLDG-A1", "patterns", List.of(), "peakHours", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("18.5%", result.getOutputData().get("projectedSavings"));
    }

    @Test
    void returnsRecommendations() {
        Task task = taskWith(Map.of("buildingId", "BLDG-A1", "patterns", List.of(), "peakHours", List.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> recs = (List<String>) result.getOutputData().get("recommendations");
        assertNotNull(recs);
        assertEquals(3, recs.size());
    }

    @Test
    void recommendationsContainHvacAdvice() {
        Task task = taskWith(Map.of("buildingId", "BLDG-A1", "patterns", List.of(), "peakHours", List.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> recs = (List<String>) result.getOutputData().get("recommendations");
        assertTrue(recs.stream().anyMatch(r -> r.contains("HVAC")));
    }

    @Test
    void handlesMissingBuildingId() {
        Task task = taskWith(Map.of("patterns", List.of(), "peakHours", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("projectedSavings"));
    }

    @Test
    void handlesVariousBuildingIds() {
        Task task = taskWith(Map.of("buildingId", "FACTORY-12", "patterns", List.of(), "peakHours", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of("buildingId", "BLDG-C3", "patterns", List.of(), "peakHours", List.of()));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("projectedSavings"));
        assertTrue(result.getOutputData().containsKey("recommendations"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
