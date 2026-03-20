package gracefulserviceshutdown.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeregisterWorkerTest {

    private final DeregisterWorker worker = new DeregisterWorker();

    @Test
    void taskDefName() {
        assertEquals("gs_deregister", worker.getTaskDefName());
    }

    @Test
    void deregistersSuccessfully() {
        Task task = taskWith(Map.of("serviceName", "order-service", "instanceId", "pod-1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deregistered"));
    }

    @Test
    void outputContainsServiceName() {
        Task task = taskWith(Map.of("serviceName", "payment-service", "instanceId", "pod-2"));
        TaskResult result = worker.execute(task);

        assertEquals("payment-service", result.getOutputData().get("serviceName"));
    }

    @Test
    void outputContainsInstanceId() {
        Task task = taskWith(Map.of("serviceName", "order-service", "instanceId", "pod-5"));
        TaskResult result = worker.execute(task);

        assertEquals("pod-5", result.getOutputData().get("instanceId"));
    }

    @Test
    void handlesNullServiceName() {
        Map<String, Object> input = new HashMap<>();
        input.put("serviceName", null);
        input.put("instanceId", "pod-3");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown-service", result.getOutputData().get("serviceName"));
    }

    @Test
    void handlesNullInstanceId() {
        Map<String, Object> input = new HashMap<>();
        input.put("serviceName", "order-service");
        input.put("instanceId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown-instance", result.getOutputData().get("instanceId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deregistered"));
    }

    @Test
    void handlesVariousServiceNames() {
        Task task = taskWith(Map.of("serviceName", "inventory-service", "instanceId", "inv-pod-7"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("inventory-service", result.getOutputData().get("serviceName"));
        assertEquals("inv-pod-7", result.getOutputData().get("instanceId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
