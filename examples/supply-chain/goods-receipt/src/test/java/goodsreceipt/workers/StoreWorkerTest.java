package goodsreceipt.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StoreWorkerTest {
    private final StoreWorker worker = new StoreWorker();

    @Test void taskDefName() { assertEquals("grc_store", worker.getTaskDefName()); }

    @Test void storesItems() {
        Task task = taskWith(Map.of("inspectedItems", List.of(Map.of("sku", "A")), "matched", true));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("WH-A-12", result.getOutputData().get("location"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
