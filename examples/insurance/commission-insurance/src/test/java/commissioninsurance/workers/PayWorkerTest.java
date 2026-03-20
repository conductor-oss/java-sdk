package commissioninsurance.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PayWorkerTest {

    @Test
    void testPayWorker() {
        PayWorker worker = new PayWorker();
        assertEquals("cin_pay", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("agentId", "AGT-789", "netCommission", "430"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("PAY-commission-insurance-001", result.getOutputData().get("paymentId"));
    }
}
