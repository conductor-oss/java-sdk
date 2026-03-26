package purchaseorder.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ApproveWorkerTest {
    private final ApproveWorker worker = new ApproveWorker();

    @Test void taskDefName() { assertEquals("po_approve", worker.getTaskDefName()); }

    @Test void approvesWithinLimit() {
        Task task = taskWith(Map.of("poNumber", "PO-001", "totalAmount", 50000));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("approved"));
    }

    @Test void escalatesOverLimit() {
        Task task = taskWith(Map.of("poNumber", "PO-001", "totalAmount", 200000));
        TaskResult result = worker.execute(task);
        assertEquals(false, result.getOutputData().get("approved"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
