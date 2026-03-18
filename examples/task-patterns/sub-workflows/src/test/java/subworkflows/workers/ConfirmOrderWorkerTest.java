package subworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmOrderWorkerTest {

    private final ConfirmOrderWorker worker = new ConfirmOrderWorker();

    @Test
    void taskDefName() {
        assertEquals("sub_confirm_order", worker.getTaskDefName());
    }

    @Test
    void confirmsOrder() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-001",
                "transactionId", "TXN-ORD-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-001", result.getOutputData().get("orderId"));
        assertEquals("TXN-ORD-001", result.getOutputData().get("transactionId"));
        assertEquals(true, result.getOutputData().get("confirmed"));
    }

    @Test
    void defaultsOrderIdWhenMissing() {
        Task task = taskWith(Map.of("transactionId", "TXN-123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("UNKNOWN", result.getOutputData().get("orderId"));
        assertEquals("TXN-123", result.getOutputData().get("transactionId"));
        assertEquals(true, result.getOutputData().get("confirmed"));
    }

    @Test
    void defaultsTransactionIdWhenMissing() {
        Task task = taskWith(Map.of("orderId", "ORD-002"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-002", result.getOutputData().get("orderId"));
        assertEquals("NONE", result.getOutputData().get("transactionId"));
        assertEquals(true, result.getOutputData().get("confirmed"));
    }

    @Test
    void defaultsBothWhenEmpty() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("UNKNOWN", result.getOutputData().get("orderId"));
        assertEquals("NONE", result.getOutputData().get("transactionId"));
        assertEquals(true, result.getOutputData().get("confirmed"));
    }

    @Test
    void defaultsBlankOrderId() {
        Task task = taskWith(new HashMap<>(Map.of(
                "orderId", "  ",
                "transactionId", "TXN-X")));
        TaskResult result = worker.execute(task);

        assertEquals("UNKNOWN", result.getOutputData().get("orderId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
