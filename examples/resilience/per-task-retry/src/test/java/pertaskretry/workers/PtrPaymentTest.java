package pertaskretry.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PtrPaymentTest {

    private final PtrPayment worker = new PtrPayment();

    @Test
    void taskDefName() {
        assertEquals("ptr_payment", worker.getTaskDefName());
    }

    @Test
    void returnsChargedResult() {
        Task task = taskWith("ORD-002", 0);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("charged", result.getOutputData().get("result"));
        assertEquals("ORD-002", result.getOutputData().get("orderId"));
    }

    @Test
    void attemptReflectsRetryCount() {
        Task task = taskWith("ORD-003", 0);
        TaskResult result1 = worker.execute(task);
        assertEquals(1, result1.getOutputData().get("attempt"));

        Task task2 = taskWith("ORD-003", 2);
        TaskResult result2 = worker.execute(task2);
        assertEquals(3, result2.getOutputData().get("attempt"));
    }

    @Test
    void outputContainsExpectedFields() {
        Task task = taskWith("ORD-FIELDS", 0);

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("attempt"));
        assertTrue(result.getOutputData().containsKey("orderId"));
    }

    @Test
    void handlesNullOrderId() {
        Task task = taskWith(null, 0);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("charged", result.getOutputData().get("result"));
        assertNull(result.getOutputData().get("orderId"));
    }

    private Task taskWith(String orderId, int retryCount) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setRetryCount(retryCount);
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", orderId);
        task.setInputData(input);
        return task;
    }
}
