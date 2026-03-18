package contentenricher.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveMessageWorkerTest {

    private final ReceiveMessageWorker worker = new ReceiveMessageWorker();

    @Test
    void taskDefName() {
        assertEquals("enr_receive_message", worker.getTaskDefName());
    }

    @Test
    void receivesMessageAndExtractsCustomerId() {
        Task task = taskWith(Map.of("message", Map.of("customerId", "CUST-42", "orderId", "ORD-999")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("CUST-42", result.getOutputData().get("customerId"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void passesFullMessageThrough() {
        Map<String, Object> msg = Map.of("customerId", "C1", "orderId", "O1", "amount", 100);
        Task task = taskWith(Map.of("message", msg));
        TaskResult result = worker.execute(task);

        Map<String, Object> outMsg = (Map<String, Object>) result.getOutputData().get("message");
        assertEquals("C1", outMsg.get("customerId"));
        assertEquals("O1", outMsg.get("orderId"));
    }

    @Test
    void handlesNullMessage() {
        Map<String, Object> input = new HashMap<>();
        input.put("message", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("customerId"));
    }

    @Test
    void handlesMissingCustomerId() {
        Task task = taskWith(Map.of("message", Map.of("orderId", "ORD-1")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("customerId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("customerId"));
    }

    @Test
    void handlesNumericCustomerId() {
        Task task = taskWith(Map.of("message", Map.of("customerId", 12345)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("12345", result.getOutputData().get("customerId"));
    }

    @Test
    void outputContainsMessageKey() {
        Task task = taskWith(Map.of("message", Map.of("customerId", "X")));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("message"));
        assertNotNull(result.getOutputData().get("customerId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
