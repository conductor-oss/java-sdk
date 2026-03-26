package interservicecommunication.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class InventoryServiceWorkerTest {
    private final InventoryServiceWorker worker = new InventoryServiceWorker();
    @Test void taskDefName() { assertEquals("isc_inventory_service", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("orderRef", "REF-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("warehouse"));
    }
    @Test void handlesMissingInputs() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>());
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(t).getStatus());
    }
}
