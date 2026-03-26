package returnsprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RefundWorkerTest {

    private final RefundWorker worker = new RefundWorker();

    @Test
    void taskDefName() { assertEquals("ret_refund", worker.getTaskDefName()); }

    @Test
    void returnsRefundId() {
        Task task = taskWith(Map.of("returnId", "RET-1", "orderId", "ORD-1", "amount", 129.99, "customerId", "c1"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("refundId").toString().startsWith("REF-"));
        assertEquals(true, r.getOutputData().get("refunded"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
