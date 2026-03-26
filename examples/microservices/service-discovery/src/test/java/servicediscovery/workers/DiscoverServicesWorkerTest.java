package servicediscovery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiscoverServicesWorkerTest {

    private final DiscoverServicesWorker worker = new DiscoverServicesWorker();

    @Test
    void taskDefName() {
        assertEquals("sd_discover_services", worker.getTaskDefName());
    }

    @Test
    void discoversInstances() {
        Task task = taskWith(Map.of("serviceName", "order-service"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> instances = (List<Map<String, Object>>) result.getOutputData().get("instances");
        assertEquals(3, instances.size());
    }

    @Test
    void firstInstanceIsHealthy() {
        Task task = taskWith(Map.of("serviceName", "order-service"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> instances = (List<Map<String, Object>>) result.getOutputData().get("instances");
        assertEquals("healthy", instances.get(0).get("health"));
        assertEquals("inst-01", instances.get(0).get("id"));
    }

    @Test
    void thirdInstanceIsDegraded() {
        Task task = taskWith(Map.of("serviceName", "order-service"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> instances = (List<Map<String, Object>>) result.getOutputData().get("instances");
        assertEquals("degraded", instances.get(2).get("health"));
    }

    @Test
    void returnsRegistrySource() {
        Task task = taskWith(Map.of("serviceName", "order-service"));
        TaskResult result = worker.execute(task);

        assertEquals("consul", result.getOutputData().get("registrySource"));
    }

    @Test
    void handlesNullServiceName() {
        Map<String, Object> input = new HashMap<>();
        input.put("serviceName", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingServiceName() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void instancesHaveConnectionCounts() {
        Task task = taskWith(Map.of("serviceName", "order-service"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> instances = (List<Map<String, Object>>) result.getOutputData().get("instances");
        assertEquals(12, instances.get(0).get("connections"));
        assertEquals(5, instances.get(1).get("connections"));
        assertEquals(30, instances.get(2).get("connections"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
