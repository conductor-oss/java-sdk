package goodsreceipt.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReceiveWorkerTest {
    private final ReceiveWorker worker = new ReceiveWorker();

    @Test void taskDefName() { assertEquals("grc_receive", worker.getTaskDefName()); }

    @Test void receivesShipment() {
        Task task = taskWith(Map.of("shipmentId", "SHP-001", "items", List.of(Map.of("sku", "A", "qty", 10))));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("GR-655-001", result.getOutputData().get("receiptId"));
    }

    @Test void handlesNullItems() {
        Map<String, Object> input = new HashMap<>(); input.put("shipmentId", "SHP-001"); input.put("items", null);
        TaskResult result = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
