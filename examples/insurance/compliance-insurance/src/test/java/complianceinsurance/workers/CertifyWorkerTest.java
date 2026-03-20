package complianceinsurance.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CertifyWorkerTest {

    @Test
    void testCertifyWorker() {
        CertifyWorker worker = new CertifyWorker();
        assertEquals("cpi_certify", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("companyId", "INS-CO-707", "allResolved", "true"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("compliant", result.getOutputData().get("complianceStatus"));
    }
}
