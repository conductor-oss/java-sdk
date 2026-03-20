package warehousemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PutAwayWorkerTest {
    @Test void taskDefName() { assertEquals("wm_put_away", new PutAwayWorker().getTaskDefName()); }
    @Test void assignsLocations() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("receivedItems", List.of(Map.of("sku","A"),Map.of("sku","B")))));
        TaskResult r = new PutAwayWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(2, ((List<?>) r.getOutputData().get("locations")).size());
    }
}
