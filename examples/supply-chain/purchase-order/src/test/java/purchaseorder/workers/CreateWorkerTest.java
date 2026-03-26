package purchaseorder.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CreateWorkerTest {
    private final CreateWorker worker = new CreateWorker();

    @Test void taskDefName() { assertEquals("po_create", worker.getTaskDefName()); }

    @Test void createsPO() {
        Task task = taskWith(Map.of("vendor", "Acme", "totalAmount", 12500));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("PO-654-001", result.getOutputData().get("poNumber"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
