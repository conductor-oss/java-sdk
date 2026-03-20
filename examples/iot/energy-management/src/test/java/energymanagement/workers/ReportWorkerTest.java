package energymanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReportWorkerTest {

    private final ReportWorker worker = new ReportWorker();

    @Test
    void taskDefName() {
        assertEquals("erg_report", worker.getTaskDefName());
    }

    @Test
    void returnsReportId() {
        Task task = taskWith(Map.of("buildingId", "BLDG-A1", "consumption", "data", "savings", "18.5%"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("reportId"));
        assertTrue(((String) result.getOutputData().get("reportId")).startsWith("RPT-"));
    }

    @Test
    void returnsGenerated() {
        Task task = taskWith(Map.of("buildingId", "BLDG-A1", "savings", "18.5%"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("generated"));
    }

    @Test
    void handlesMissingSavings() {
        Task task = taskWith(Map.of("buildingId", "BLDG-A1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingBuildingId() {
        Task task = taskWith(Map.of("savings", "10%"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("reportId"));
    }

    @Test
    void reportIdIsDeterministic() {
        Task task1 = taskWith(Map.of("buildingId", "BLDG-A1", "savings", "18.5%"));
        Task task2 = taskWith(Map.of("buildingId", "BLDG-B2", "savings", "12%"));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("reportId"), r2.getOutputData().get("reportId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
