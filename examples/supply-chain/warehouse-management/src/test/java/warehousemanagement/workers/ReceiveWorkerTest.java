package warehousemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReceiveWorkerTest {
    @Test void taskDefName() { assertEquals("wm_receive", new ReceiveWorker().getTaskDefName()); }
    @Test void receivesItems() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("orderId","O-1","items",List.of(Map.of("sku","A")))));
        TaskResult r = new ReceiveWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("receivedItems"));
    }
}
