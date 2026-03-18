package dataaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatReportWorkerTest {

    private final FormatReportWorker worker = new FormatReportWorker();

    @Test
    void taskDefName() {
        assertEquals("agg_format_report", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void formatsSingleGroupReport() {
        Map<String, Map<String, Object>> aggregates = new LinkedHashMap<>();
        aggregates.put("east", Map.of("count", 2, "sum", 300.0, "avg", 150.0, "min", 100.0, "max", 200.0));

        Task task = taskWith(Map.of("aggregates", aggregates));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<String> report = (List<String>) result.getOutputData().get("report");
        assertEquals(1, report.size());
        assertEquals("east: count=2, sum=300.0, avg=150.0, min=100.0, max=200.0", report.get(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void formatsMultipleGroupsReport() {
        Map<String, Map<String, Object>> aggregates = new LinkedHashMap<>();
        aggregates.put("east", Map.of("count", 2, "sum", 300.0, "avg", 150.0, "min", 100.0, "max", 200.0));
        aggregates.put("west", Map.of("count", 1, "sum", 200.0, "avg", 200.0, "min", 200.0, "max", 200.0));

        Task task = taskWith(Map.of("aggregates", aggregates));
        TaskResult result = worker.execute(task);

        List<String> report = (List<String>) result.getOutputData().get("report");
        assertEquals(2, report.size());
        assertTrue(report.get(0).startsWith("east:"));
        assertTrue(report.get(1).startsWith("west:"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlesEmptyAggregates() {
        Task task = taskWith(Map.of("aggregates", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<String> report = (List<String>) result.getOutputData().get("report");
        assertTrue(report.isEmpty());
    }

    @Test
    void handlesNullAggregates() {
        Map<String, Object> input = new HashMap<>();
        input.put("aggregates", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportLineContainsCount() {
        Map<String, Map<String, Object>> aggregates = new LinkedHashMap<>();
        aggregates.put("north", Map.of("count", 5, "sum", 500.0, "avg", 100.0, "min", 50.0, "max", 200.0));

        Task task = taskWith(Map.of("aggregates", aggregates));
        TaskResult result = worker.execute(task);

        List<String> report = (List<String>) result.getOutputData().get("report");
        assertTrue(report.get(0).contains("count=5"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportLineContainsSum() {
        Map<String, Map<String, Object>> aggregates = new LinkedHashMap<>();
        aggregates.put("south", Map.of("count", 3, "sum", 750.0, "avg", 250.0, "min", 200.0, "max", 300.0));

        Task task = taskWith(Map.of("aggregates", aggregates));
        TaskResult result = worker.execute(task);

        List<String> report = (List<String>) result.getOutputData().get("report");
        assertTrue(report.get(0).contains("sum=750.0"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportLineContainsAvg() {
        Map<String, Map<String, Object>> aggregates = new LinkedHashMap<>();
        aggregates.put("central", Map.of("count", 4, "sum", 400.0, "avg", 100.0, "min", 50.0, "max", 150.0));

        Task task = taskWith(Map.of("aggregates", aggregates));
        TaskResult result = worker.execute(task);

        List<String> report = (List<String>) result.getOutputData().get("report");
        assertTrue(report.get(0).contains("avg=100.0"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportLineContainsMinMax() {
        Map<String, Map<String, Object>> aggregates = new LinkedHashMap<>();
        aggregates.put("group1", Map.of("count", 3, "sum", 600.0, "avg", 200.0, "min", 100.0, "max", 300.0));

        Task task = taskWith(Map.of("aggregates", aggregates));
        TaskResult result = worker.execute(task);

        List<String> report = (List<String>) result.getOutputData().get("report");
        assertTrue(report.get(0).contains("min=100.0"));
        assertTrue(report.get(0).contains("max=300.0"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
