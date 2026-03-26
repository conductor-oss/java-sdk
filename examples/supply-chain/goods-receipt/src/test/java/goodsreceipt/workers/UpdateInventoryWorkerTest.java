package goodsreceipt.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UpdateInventoryWorkerTest {
    private final UpdateInventoryWorker worker = new UpdateInventoryWorker();

    @Test void taskDefName() { assertEquals("grc_update_inventory", worker.getTaskDefName()); }

    @Test void updatesInventory() {
        Task task = taskWith(Map.of("storedItems", List.of(Map.of("sku", "A"), Map.of("sku", "B"))));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("updated"));
        assertEquals(2, result.getOutputData().get("itemsUpdated"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
