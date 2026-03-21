package interservicecommunication.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OrderServiceWorkerTest {
    private final OrderServiceWorker worker = new OrderServiceWorker();
    @Test void taskDefName() { assertEquals("isc_order_service", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("orderId", "ORD-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("orderRef"));
    }
    @Test void handlesMissingInputs() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>());
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(t).getStatus());
    }
}
