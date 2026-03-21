package foodordering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderWorkerTest {

    @Test
    void testExecute() {
        OrderWorker worker = new OrderWorker();
        assertEquals("fod_order", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("customerId", "CUST-42"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-5001", result.getOutputData().get("orderId"));
        assertEquals(24.98, result.getOutputData().get("total"));
    }
}
