package scheduledreports.workers;

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
        assertEquals("sch_format_report", worker.getTaskDefName());
    }

    @Test
    void formatsReportSuccessfully() {
        Task task = taskWith(Map.of("reportType", "weekly-sales", "data", Map.of("revenue", 145000), "rowCount", 3200));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("reportUrl"));
    }

    @Test
    void returnsReportUrl() {
        Task task = taskWith(Map.of("reportType", "weekly-sales", "rowCount", 3200));
        TaskResult result = worker.execute(task);

        assertEquals("https://reports.example.com/weekly-sales-20260308.pdf", result.getOutputData().get("reportUrl"));
    }

    @Test
    void returnsFormatPdf() {
        Task task = taskWith(Map.of("reportType", "weekly-sales", "rowCount", 3200));
        TaskResult result = worker.execute(task);

        assertEquals("PDF", result.getOutputData().get("format"));
    }

    @Test
    void returnsSizeKb() {
        Task task = taskWith(Map.of("reportType", "weekly-sales", "rowCount", 3200));
        TaskResult result = worker.execute(task);

        assertEquals(342, result.getOutputData().get("sizeKb"));
    }

    @Test
    void returnsPages() {
        Task task = taskWith(Map.of("reportType", "weekly-sales", "rowCount", 3200));
        TaskResult result = worker.execute(task);

        assertEquals(8, result.getOutputData().get("pages"));
    }

    @Test
    void handlesNullReportType() {
        Map<String, Object> input = new HashMap<>();
        input.put("reportType", null);
        input.put("rowCount", 100);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
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
