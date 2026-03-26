package customerchurn.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DetectRiskWorkerTest {

    @Test
    void testDetectRiskWorker() {
        DetectRiskWorker worker = new DetectRiskWorker();
        assertEquals("ccn_detect_risk", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("customerId", "CUST-812", "usageTrend", "declining"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.82, result.getOutputData().get("riskScore"));
    }
}
