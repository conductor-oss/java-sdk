package tradeexecution.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateOrderWorkerTest {

    private final ValidateOrderWorker worker = new ValidateOrderWorker();

    @Test
    void taskDefName() {
        assertEquals("trd_validate_order", worker.getTaskDefName());
    }

    @Test
    void validatesOrderSuccessfully() {
        Task task = taskWith(Map.of("orderId", "ORD-1", "side", "BUY", "quantity", 100, "symbol", "AAPL"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
        assertEquals(true, result.getOutputData().get("sufficientFunds"));
    }

    @Test
    void returnsBuyingPower() {
        Task task = taskWith(Map.of("orderId", "ORD-2", "side", "SELL", "quantity", 50, "symbol", "MSFT"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("buyingPower"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
