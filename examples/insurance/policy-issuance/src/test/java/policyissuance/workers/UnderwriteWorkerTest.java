package policyissuance.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class UnderwriteWorkerTest {

    @Test
    void testUnderwriteWorker() {
        UnderwriteWorker worker = new UnderwriteWorker();
        assertEquals("pis_underwrite", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("applicantId", "APP-703", "coverageType", "auto"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("approved", result.getOutputData().get("result"));
    }
}
