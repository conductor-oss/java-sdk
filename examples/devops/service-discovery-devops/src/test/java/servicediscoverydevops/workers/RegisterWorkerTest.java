package servicediscoverydevops.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RegisterWorkerTest {

    private final RegisterWorker worker = new RegisterWorker();

    @Test
    void taskDefName() {
        assertEquals("sd_register", worker.getTaskDefName());
    }

    @Test
    void registrationIdIsDeterministic() {
        Task task = taskWith("payment-service", "http://pay-svc:8080", "1.2.0");

        TaskResult r1 = worker.execute(task);
        TaskResult r2 = worker.execute(task);

        assertEquals(r1.getOutputData().get("registrationId"),
                     r2.getOutputData().get("registrationId"),
                     "Registration ID must be deterministic for the same inputs");
    }

    @Test
    void registrationIdDerivedFromInputs() {
        Task task = taskWith("order-service", "http://order-svc:8080", "3.1.0");

        TaskResult result = worker.execute(task);

        assertEquals("reg-order-service-3.1.0", result.getOutputData().get("registrationId"));
    }

    @Test
    void completedWithExpectedOutputFields() {
        Task task = taskWith("user-api", "http://user-api.prod.internal:8080", "2.0.1");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("reg-user-api-2.0.1", result.getOutputData().get("registrationId"));
        assertEquals("user-api", result.getOutputData().get("serviceName"));
        assertEquals("http://user-api.prod.internal:8080", result.getOutputData().get("endpoint"));
        assertEquals("2.0.1", result.getOutputData().get("version"));
        assertEquals("registered", result.getOutputData().get("status"));
    }

    @Test
    void performsRealDnsLookupForKnownHost() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWith("google-proxy", "https://google.com:443", "1.0.0");
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("dnsResolved"));

        @SuppressWarnings("unchecked")
        List<String> addresses = (List<String>) result.getOutputData().get("resolvedAddresses");
        assertNotNull(addresses);
        assertFalse(addresses.isEmpty(), "Should resolve google.com to at least one IP");
    }

    @Test
    void handlesDnsFailureGracefully() {
        Task task = taskWith("test-svc", "http://nonexistent.invalid:8080", "1.0.0");
        TaskResult result = worker.execute(task);

        // Should still complete even if DNS fails
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("registered", result.getOutputData().get("status"));
        assertEquals(false, result.getOutputData().get("dnsResolved"));
    }

    @Test
    void extractsHostnameCorrectly() {
        assertEquals("example.com", RegisterWorker.extractHostname("http://example.com:8080/path"));
        assertEquals("localhost", RegisterWorker.extractHostname("http://localhost:3000"));
        assertEquals("google.com", RegisterWorker.extractHostname("https://google.com"));
    }

    @Test
    void defaultsAppliedWhenInputsMissing() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("reg-unknown-service-0.0.0", result.getOutputData().get("registrationId"));
        assertEquals("unknown-service", result.getOutputData().get("serviceName"));
    }

    private static boolean isNetworkAvailable() {
        try {
            InetAddress.getByName("google.com");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Task taskWith(String serviceName, String endpoint, String version) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("serviceName", serviceName);
        input.put("endpoint", endpoint);
        input.put("version", version);
        task.setInputData(input);
        return task;
    }
}
