package servicediscoverydevops.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NotifyConsumersWorkerTest {

    private final NotifyConsumersWorker worker = new NotifyConsumersWorker();

    @Test
    void taskDefName() {
        assertEquals("sd_notify_consumers", worker.getTaskDefName());
    }

    @Test
    void notifiesConsumersWithRealDnsLookups() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWithData("payment-service");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("notified"));

        int successCount = ((Number) result.getOutputData().get("successCount")).intValue();
        assertTrue(successCount > 0, "Should resolve at least some consumer hosts via DNS");
    }

    @Test
    void consumerCountMatchesList() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWithData("order-service");

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> consumers = (List<Map<String, Object>>) result.getOutputData().get("consumers");
        int count = ((Number) result.getOutputData().get("consumerCount")).intValue();

        assertNotNull(consumers, "consumers list must be present");
        assertFalse(consumers.isEmpty(), "consumers list must not be empty");
        assertEquals(consumers.size(), count, "consumerCount must match consumers list size");
    }

    @Test
    void consumerEntriesContainDnsResults() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWithData("test-service");

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> consumers = (List<Map<String, Object>>) result.getOutputData().get("consumers");

        for (Map<String, Object> consumer : consumers) {
            assertNotNull(consumer.get("name"));
            assertNotNull(consumer.get("host"));
            assertNotNull(consumer.get("dnsResolved"));
            if (Boolean.TRUE.equals(consumer.get("dnsResolved"))) {
                assertNotNull(consumer.get("resolvedAddress"), "Resolved consumers should have an IP address");
            }
        }
    }

    @Test
    void outputIncludesNotificationMethod() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWithData("user-api");

        TaskResult result = worker.execute(task);

        assertEquals("webhook", result.getOutputData().get("notificationMethod"));
    }

    @Test
    void outputIncludesServiceName() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWithData("analytics-engine");

        TaskResult result = worker.execute(task);

        assertEquals("analytics-engine", result.getOutputData().get("serviceName"));
    }

    @Test
    void writesNotificationLogFile() {
        assumeTrue(isNetworkAvailable(), "Skipping — no network connectivity");
        Task task = taskWithData("log-test-service");

        TaskResult result = worker.execute(task);

        String notifFile = (String) result.getOutputData().get("notificationFile");
        assertNotNull(notifFile, "Should write a notification log file");
        assertTrue(Files.exists(Path.of(notifFile)), "Notification file should exist on disk");
    }

    @Test
    void handlesNullInputDataGracefully() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("notify_consumersData", null);
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("serviceName"));
    }

    private static boolean isNetworkAvailable() {
        try { InetAddress.getByName("google.com"); return true; } catch (Exception e) { return false; }
    }

    private Task taskWithData(String serviceName) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> data = new HashMap<>();
        data.put("serviceName", serviceName);
        Map<String, Object> input = new HashMap<>();
        input.put("notify_consumersData", data);
        task.setInputData(input);
        return task;
    }
}
