package shippingworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeliverShipmentWorkerTest {

    private final DeliverShipmentWorker worker = new DeliverShipmentWorker();

    @Test
    void taskDefName() { assertEquals("shp_deliver", worker.getTaskDefName()); }

    @Test
    void deliversShipment() {
        Task task = taskWith(Map.of("trackingNumber", "FX123", "destination", Map.of("city", "Austin")));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("delivered"));
        assertNotNull(r.getOutputData().get("deliveredAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
