package stripeintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SendReceiptWorkerTest {

    private final SendReceiptWorker worker = new SendReceiptWorker();

    @Test
    void taskDefName() {
        assertEquals("stp_send_receipt", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("email", "test@example.com", "chargeId", "ch_abc", "amount", 4999, "currency", "usd"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
