package aimodelevaluation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReportWorkerTest {

    @Test
    void testReportWorker() {
        ReportWorker worker = new ReportWorker();
        assertEquals("ame_report", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("modelId", "MDL-TEST", "metrics", "test"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("EVAL-ai-model-evaluation-001", result.getOutputData().get("reportId"));
        assertEquals(true, result.getOutputData().get("generated"));
    }
}
