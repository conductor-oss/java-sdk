package abandonedcart.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DetectAbandonmentWorkerTest {
    private final DetectAbandonmentWorker w = new DetectAbandonmentWorker();
    @Test void taskDefName() { assertEquals("abc_detect_abandonment", w.getTaskDefName()); }
    @Test void returnsItems() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("cartId", "C1", "customerId", "U1", "cartTotal", 50.0)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("items") instanceof List);
    }
}
