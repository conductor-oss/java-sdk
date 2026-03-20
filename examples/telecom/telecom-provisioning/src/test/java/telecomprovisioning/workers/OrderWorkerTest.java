package telecomprovisioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OrderWorkerTest {
    @Test
    void testOrderWorker() {
        OrderWorker worker = new OrderWorker();
        assertEquals("tpv_order", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("customerId", "CUST-815", "serviceType", "fiber-internet"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-telecom-provisioning-001", result.getOutputData().get("orderId"));
    }
}
