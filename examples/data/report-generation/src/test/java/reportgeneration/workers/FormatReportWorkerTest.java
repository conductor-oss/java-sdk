package reportgeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatReportWorkerTest {

    private final FormatReportWorker worker = new FormatReportWorker();

    @Test
    void taskDefName() {
        assertEquals("rg_format_report", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of(
                "aggregated", Map.of("totalRevenue", "$100"),
                "dateRange", Map.of("start", "2024-03-01", "end", "2024-03-07")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsFormatPdf() {
        Task task = taskWith(Map.of("aggregated", Map.of(), "dateRange", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals("PDF", result.getOutputData().get("format"));
    }

    @Test
    void returnsReportUrl() {
        Task task = taskWith(Map.of("aggregated", Map.of(), "dateRange", Map.of()));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("reportUrl"));
    }

    @Test
    void returnsReport() {
        Task task = taskWith(Map.of(
                "aggregated", Map.of("totalRevenue", "$100"),
                "dateRange", Map.of("start", "2024-03-01", "end", "2024-03-07")));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("report"));
    }

    @Test
    void returnsPageCount() {
        Task task = taskWith(Map.of("aggregated", Map.of(), "dateRange", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(8, result.getOutputData().get("pageCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
