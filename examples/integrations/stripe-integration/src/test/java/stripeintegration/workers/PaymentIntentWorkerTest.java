package stripeintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaymentIntentWorkerTest {

    private final PaymentIntentWorker worker = new PaymentIntentWorker();

    @Test
    void taskDefName() {
        assertEquals("stp_payment_intent", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("customerId", "cus_abc", "amount", 4999, "currency", "usd", "description", "test"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("paymentIntentId"));
        assertEquals("requires_capture", result.getOutputData().get("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
