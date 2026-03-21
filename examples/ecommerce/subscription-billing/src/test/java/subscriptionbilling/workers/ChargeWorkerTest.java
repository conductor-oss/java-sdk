package subscriptionbilling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ChargeWorkerTest {
    private final ChargeWorker worker = new ChargeWorker();

    @Test void taskDefName() { assertEquals("sub_charge", worker.getTaskDefName()); }

    @Test void returnsChargeId() {
        Task task = taskWith(Map.of("invoiceId", "INV-1", "customerId", "c1", "amount", 29.99));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("chargeId").toString().startsWith("chg-"));
        assertEquals(true, r.getOutputData().get("charged"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
