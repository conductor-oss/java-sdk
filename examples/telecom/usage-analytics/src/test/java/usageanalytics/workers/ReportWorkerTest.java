package usageanalytics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReportWorkerTest {

    @Test
    void testReportWorker() {
        ReportWorker worker = new ReportWorker();
        assertEquals("uag_report", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("aggregates", "data", "region", "WEST-COAST"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("RPT-usage-analytics-001", result.getOutputData().get("reportId"));
    }
}
