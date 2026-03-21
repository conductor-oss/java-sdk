package actuarialworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReportWorkerTest {

    @Test
    void testReportWorker() {
        ReportWorker worker = new ReportWorker();
        assertEquals("act_report", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("analysis", "data", "lineOfBusiness", "commercial-property"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("RPT-actuarial-workflow-001", result.getOutputData().get("reportId"));
    }
}
