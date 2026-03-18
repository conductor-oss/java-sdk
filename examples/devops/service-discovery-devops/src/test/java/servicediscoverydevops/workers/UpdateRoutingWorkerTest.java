package servicediscoverydevops.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class UpdateRoutingWorkerTest {

    private final UpdateRoutingWorker worker = new UpdateRoutingWorker();

    @Test
    void taskDefName() {
        assertEquals("sd_update_routing", worker.getTaskDefName());
    }

    @Test
    void routingUpdateOutputs() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWithData("payment-service", "https://www.google.com");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("updated"));
        assertEquals("round-robin", result.getOutputData().get("routingStrategy"));
        assertEquals("envoy-proxy", result.getOutputData().get("loadBalancer"));
    }

    @Test
    void createsRealRoutingFile() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWithData("test-routing-svc", "https://www.google.com");

        TaskResult result = worker.execute(task);

        String routingFile = (String) result.getOutputData().get("routingFile");
        assertNotNull(routingFile, "routingFile should be present");
        assertTrue(Files.exists(Path.of(routingFile)), "Routing file should exist on disk");
    }

    @Test
    void activeInstancesIsPositive() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWithData("order-service", "https://www.google.com");

        TaskResult result = worker.execute(task);

        Object instances = result.getOutputData().get("activeInstances");
        assertNotNull(instances, "activeInstances must be present");
        assertInstanceOf(Number.class, instances);
        assertTrue(((Number) instances).intValue() > 0, "activeInstances must be positive");
    }

    @Test
    void outputIncludesServiceAndEndpoint() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWithData("user-api", "https://github.com");

        TaskResult result = worker.execute(task);

        assertEquals("user-api", result.getOutputData().get("serviceName"));
        assertEquals("https://github.com", result.getOutputData().get("endpoint"));
    }

    @Test
    void performsRealDnsResolution() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWithData("dns-test-svc", "https://www.google.com");

        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("routeAddress"));
        // google.com should resolve to an IP (not "google.com")
        String routeAddress = (String) result.getOutputData().get("routeAddress");
        assertFalse(routeAddress.contains("google"), "Should resolve to an IP address, not hostname");
    }

    @Test
    void handlesNullInputDataGracefully() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("update_routingData", null);
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("serviceName"));
    }

    private static boolean isNetworkAvailable() {
        try { InetAddress.getByName("google.com"); return true; } catch (Exception e) { return false; }
    }

    private Task taskWithData(String serviceName, String endpoint) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> data = new HashMap<>();
        data.put("serviceName", serviceName);
        data.put("endpoint", endpoint);
        data.put("healthy", true);
        data.put("registrationId", "reg-" + serviceName + "-1.0.0");
        Map<String, Object> input = new HashMap<>();
        input.put("update_routingData", data);
        task.setInputData(input);
        return task;
    }
}
