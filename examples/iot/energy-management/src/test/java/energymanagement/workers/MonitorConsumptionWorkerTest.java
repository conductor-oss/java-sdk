package energymanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MonitorConsumptionWorkerTest {

    private final MonitorConsumptionWorker worker = new MonitorConsumptionWorker();

    @Test
    void taskDefName() {
        assertEquals("erg_monitor_consumption", worker.getTaskDefName());
    }

    @Test
    void returnsConsumptionReadings() {
        Task task = taskWith(Map.of("buildingId", "BLDG-A1", "period", "2024-01"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Object consumption = result.getOutputData().get("consumption");
        assertNotNull(consumption);
        assertTrue(consumption instanceof List);
        assertEquals(4, ((List<?>) consumption).size());
    }

    @Test
    void returnsTotalKwh() {
        Task task = taskWith(Map.of("buildingId", "BLDG-A1", "period", "2024-01"));
        TaskResult result = worker.execute(task);

        String totalKwh = (String) result.getOutputData().get("totalKwh");
        assertNotNull(totalKwh);
        // (12.3 + 18.7 + 45.2 + 38.1) * 6 = 685.8
        assertEquals("685.8", totalKwh);
    }

    @Test
    void handlesVariousBuildingIds() {
        Task task = taskWith(Map.of("buildingId", "BLDG-Z9", "period", "2024-06"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingBuildingId() {
        Task task = taskWith(Map.of("period", "2024-01"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("totalKwh"));
    }

    @Test
    void handlesMissingPeriod() {
        Task task = taskWith(Map.of("buildingId", "BLDG-A1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("consumption"));
        assertNotNull(result.getOutputData().get("totalKwh"));
    }

    @Test
    void consumptionReadingsHaveExpectedStructure() {
        Task task = taskWith(Map.of("buildingId", "BLDG-A1", "period", "2024-01"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> readings = (List<Map<String, Object>>) result.getOutputData().get("consumption");
        Map<String, Object> first = readings.get(0);
        assertTrue(first.containsKey("hour"));
        assertTrue(first.containsKey("kw"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
