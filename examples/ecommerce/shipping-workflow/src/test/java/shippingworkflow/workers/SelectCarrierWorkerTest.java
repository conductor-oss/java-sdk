package shippingworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SelectCarrierWorkerTest {

    private final SelectCarrierWorker worker = new SelectCarrierWorker();

    @Test
    void taskDefName() { assertEquals("shp_select_carrier", worker.getTaskDefName()); }

    @Test
    void expressSelectsFedEx() {
        Task task = taskWith(Map.of("speed", "express", "weight", 3.5));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("FedEx", r.getOutputData().get("carrier"));
        assertEquals(29.99, ((Number) r.getOutputData().get("cost")).doubleValue());
    }

    @Test
    void standardSelectsUSPS() {
        Task task = taskWith(Map.of("speed", "standard", "weight", 2.0));
        TaskResult r = worker.execute(task);
        assertEquals("USPS", r.getOutputData().get("carrier"));
        assertEquals(12.99, ((Number) r.getOutputData().get("cost")).doubleValue());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
