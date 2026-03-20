package reinsurance.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AssessRiskWorkerTest {

    @Test
    void testAssessRiskWorker() {
        AssessRiskWorker worker = new AssessRiskWorker();
        assertEquals("rin_assess_risk", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("policyId", "POL-705", "coverageAmount", "5000000"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5000000, result.getOutputData().get("netExposure"));
    }
}
