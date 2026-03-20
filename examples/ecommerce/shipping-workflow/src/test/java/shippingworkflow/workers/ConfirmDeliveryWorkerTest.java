package shippingworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmDeliveryWorkerTest {

    private final ConfirmDeliveryWorker worker = new ConfirmDeliveryWorker();

    @Test
    void taskDefName() { assertEquals("shp_confirm", worker.getTaskDefName()); }

    @Test
    void confirmsDelivery() {
        Task task = taskWith(Map.of("orderId", "ORD-1", "trackingNumber", "FX123", "deliveredAt", "2024-01-01T00:00:00Z"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("confirmed"));
        assertEquals(true, r.getOutputData().get("notificationSent"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
