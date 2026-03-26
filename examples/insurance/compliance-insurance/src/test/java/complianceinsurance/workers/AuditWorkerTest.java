package complianceinsurance.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AuditWorkerTest {

    @Test
    void testAuditWorker() {
        AuditWorker worker = new AuditWorker();
        assertEquals("cpi_audit", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("companyId", "INS-CO-707", "compliancePeriod", "Q1-2024"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("findings"));
    }
}
