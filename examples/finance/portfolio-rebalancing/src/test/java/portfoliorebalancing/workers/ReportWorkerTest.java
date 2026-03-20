package portfoliorebalancing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReportWorkerTest {
    private final ReportWorker worker = new ReportWorker();

    @Test void taskDefName() { assertEquals("prt_report", worker.getTaskDefName()); }

    @Test void generatesReport() {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("tradesExecuted", 3, "verified", true)));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("RPT-REBAL-2024-001", result.getOutputData().get("reportId"));
        assertNotNull(result.getOutputData().get("generatedAt"));
    }
}
