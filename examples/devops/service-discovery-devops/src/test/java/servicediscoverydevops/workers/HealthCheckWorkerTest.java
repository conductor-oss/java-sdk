package servicediscoverydevops.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class HealthCheckWorkerTest {

    private final HealthCheckWorker worker = new HealthCheckWorker();

    @Test
    void taskDefName() {
        assertEquals("sd_health_check", worker.getTaskDefName());
    }

    @Test
    void healthCheckAgainstGoogleReturnsHealthy() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWithData("https://www.google.com", "google-service", "1.0.0", "reg-google-1.0.0");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("healthy"));
        assertEquals(true, result.getOutputData().get("dnsResolved"));
    }

    @Test
    void responseTimePresentAndNumeric() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWithData("https://www.google.com", "google-service", "1.0.0", "reg-google-1.0.0");

        TaskResult result = worker.execute(task);

        Object responseTime = result.getOutputData().get("responseTimeMs");
        assertNotNull(responseTime, "responseTimeMs must be present");
        assertInstanceOf(Number.class, responseTime, "responseTimeMs must be a number");
        assertTrue(((Number) responseTime).longValue() >= 0, "responseTimeMs must be non-negative");
    }

    @Test
    void outputContainsExpectedFields() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWithData("https://www.google.com", "google-svc", "2.0.1", "reg-google-svc-2.0.1");

        TaskResult result = worker.execute(task);

        assertEquals("https://www.google.com", result.getOutputData().get("endpoint"));
        assertEquals("google-svc", result.getOutputData().get("serviceName"));
        assertEquals("2.0.1", result.getOutputData().get("version"));
        assertEquals("reg-google-svc-2.0.1", result.getOutputData().get("registrationId"));
        assertNotNull(result.getOutputData().get("lastChecked"), "lastChecked must be present");
    }

    @Test
    void unreachableEndpointReturnsFalseHealthy() {
        Task task = taskWithData("http://nonexistent.invalid:9999", "bad-svc", "1.0.0", "reg-bad-1.0.0");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("healthy"));
        assertEquals(false, result.getOutputData().get("dnsResolved"));
    }

    @Test
    void handlesNullInputDataGracefully() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("health_checkData", null);
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("endpoint"));
    }

    @Test
    void extractPortFromUrl() {
        assertEquals(8080, HealthCheckWorker.extractPort("http://localhost:8080"));
        assertEquals(443, HealthCheckWorker.extractPort("https://example.com"));
        assertEquals(80, HealthCheckWorker.extractPort("http://example.com"));
        assertEquals(3000, HealthCheckWorker.extractPort("http://localhost:3000/api"));
    }

    private static boolean isNetworkAvailable() {
        try { InetAddress.getByName("google.com"); return true; } catch (Exception e) { return false; }
    }

    private Task taskWithData(String endpoint, String serviceName, String version, String registrationId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> data = new HashMap<>();
        data.put("endpoint", endpoint);
        data.put("serviceName", serviceName);
        data.put("version", version);
        data.put("registrationId", registrationId);
        Map<String, Object> input = new HashMap<>();
        input.put("health_checkData", data);
        task.setInputData(input);
        return task;
    }
}
