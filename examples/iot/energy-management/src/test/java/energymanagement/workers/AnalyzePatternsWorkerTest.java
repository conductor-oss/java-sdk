package energymanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzePatternsWorkerTest {

    private final AnalyzePatternsWorker worker = new AnalyzePatternsWorker();

    @Test
    void taskDefName() {
        assertEquals("erg_analyze_patterns", worker.getTaskDefName());
    }

    @Test
    void returnsPatterns() {
        Task task = taskWith(Map.of("buildingId", "BLDG-A1", "consumption", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<String> patterns = (List<String>) result.getOutputData().get("patterns");
        assertNotNull(patterns);
        assertTrue(patterns.contains("high-midday-usage"));
        assertTrue(patterns.contains("low-overnight"));
        assertTrue(patterns.contains("hvac-dominant"));
    }

    @Test
    void returnsPeakHours() {
        Task task = taskWith(Map.of("buildingId", "BLDG-A1", "consumption", List.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> peakHours = (List<String>) result.getOutputData().get("peakHours");
        assertNotNull(peakHours);
        assertTrue(peakHours.contains("11:00-15:00"));
    }

    @Test
    void returnsBaselineKw() {
        Task task = taskWith(Map.of("buildingId", "BLDG-A1", "consumption", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(12.3, result.getOutputData().get("baselineKw"));
    }

    @Test
    void handlesMissingBuildingId() {
        Task task = taskWith(Map.of("consumption", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("patterns"));
    }

    @Test
    void handlesVariousBuildingIds() {
        Task task = taskWith(Map.of("buildingId", "WAREHOUSE-7", "consumption", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("buildingId", "BLDG-B2", "consumption", List.of()));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("patterns"));
        assertTrue(result.getOutputData().containsKey("peakHours"));
        assertTrue(result.getOutputData().containsKey("baselineKw"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
