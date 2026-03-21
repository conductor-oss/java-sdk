package warehousemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PackWorkerTest {
    @Test void taskDefName() { assertEquals("wm_pack", new PackWorker().getTaskDefName()); }
    @Test void packsItems() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("pickedItems", List.of(Map.of("sku","A")))));
        TaskResult r = new PackWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("PKG-657-001", r.getOutputData().get("packageId"));
    }
}
