package foodordering.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeliverWorkerTest {

    @Test
    void testExecute() {
        DeliverWorker worker = new DeliverWorker();
        assertEquals("fod_deliver", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("orderId", "ORD-5001", "customerId", "CUST-42"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("delivery"));
    }
}
