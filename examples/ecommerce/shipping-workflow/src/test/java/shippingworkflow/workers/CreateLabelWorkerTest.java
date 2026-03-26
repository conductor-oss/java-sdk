package shippingworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreateLabelWorkerTest {

    private final CreateLabelWorker worker = new CreateLabelWorker();

    @Test
    void taskDefName() { assertEquals("shp_create_label", worker.getTaskDefName()); }

    @Test
    void fedExTrackingStartsWithFX() {
        Task task = taskWith(Map.of("orderId", "ORD-1", "carrier", "FedEx", "serviceType", "Priority"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("trackingNumber").toString().startsWith("FX"));
    }

    @Test
    void uspsTrackingStartsWithUS() {
        Task task = taskWith(Map.of("orderId", "ORD-2", "carrier", "USPS", "serviceType", "Priority Mail"));
        TaskResult r = worker.execute(task);
        assertTrue(r.getOutputData().get("trackingNumber").toString().startsWith("US"));
    }

    @Test
    void returnsLabelUrl() {
        Task task = taskWith(Map.of("orderId", "ORD-3", "carrier", "FedEx", "serviceType", "Express"));
        TaskResult r = worker.execute(task);
        assertNotNull(r.getOutputData().get("labelUrl"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
