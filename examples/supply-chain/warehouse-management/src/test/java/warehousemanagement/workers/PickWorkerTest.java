package warehousemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PickWorkerTest {
    @Test void taskDefName() { assertEquals("wm_pick", new PickWorker().getTaskDefName()); }
    @Test void picksItems() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("orderId","O-1","locations",List.of(Map.of("sku","A","location","A1")))));
        TaskResult r = new PickWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
