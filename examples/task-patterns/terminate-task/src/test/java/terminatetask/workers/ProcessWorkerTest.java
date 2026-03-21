package terminatetask.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessWorkerTest {

    private final ProcessWorker worker = new ProcessWorker();

    @Test
    void taskDefName() {
        assertEquals("term_process", worker.getTaskDefName());
    }

    @Test
    void processesOrderAndReturnsAmount() {
        Task task = taskWith(Map.of("orderId", "ORD-001", "amount", 500));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(500.0, result.getOutputData().get("processedAmount"));
    }

    @Test
    void handlesDecimalAmount() {
        Task task = taskWith(Map.of("orderId", "ORD-002", "amount", 99.99));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(99.99, result.getOutputData().get("processedAmount"));
    }

    @Test
    void handlesZeroAmount() {
        Task task = taskWith(Map.of("orderId", "ORD-003", "amount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, result.getOutputData().get("processedAmount"));
    }

    @Test
    void outputContainsProcessedAmountField() {
        Task task = taskWith(Map.of("orderId", "ORD-004", "amount", 1234));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("processedAmount"));
    }

    @Test
    void handlesNullAmount() {
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "ORD-005");
        input.put("amount", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, result.getOutputData().get("processedAmount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
