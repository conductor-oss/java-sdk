package stripeintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChargeWorkerTest {

    private final ChargeWorker worker = new ChargeWorker();

    @Test
    void taskDefName() {
        assertEquals("stp_charge", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("paymentIntentId", "pi_abc", "customerId", "cus_abc"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("chargeId"));
        assertEquals("succeeded", result.getOutputData().get("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
