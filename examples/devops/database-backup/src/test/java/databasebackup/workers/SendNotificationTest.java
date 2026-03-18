package databasebackup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SendNotificationTest {

    private final SendNotification worker = new SendNotification();

    @Test
    void taskDefName() {
        assertEquals("backup_send_notification", worker.getTaskDefName());
    }

    @Test
    void successfulBackupProducesSuccessMessage() {
        Task task = taskWith("orders_20250315T120000Z.sql.gz", 500_000_000L,
                "s3://backups/orders.sql.gz", true, 3, "1.5 GB",
                "orders_production", Map.of("channel", "slack", "recipients", List.of("dba@example.com")));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("notificationSent"));
        assertEquals("SUCCESS", result.getOutputData().get("status"));
        assertEquals("slack", result.getOutputData().get("channel"));
        assertEquals(true, result.getOutputData().get("verified"));

        String message = (String) result.getOutputData().get("message");
        assertTrue(message.contains("SUCCESS"));
        assertTrue(message.contains("orders_production"));
        assertTrue(message.contains("orders_20250315T120000Z.sql.gz"));
        assertTrue(message.contains("s3://backups/orders.sql.gz"));
        assertTrue(message.contains("Verified"));
    }

    @Test
    void failedVerificationProducesFailedMessage() {
        Task task = taskWith("orders.sql.gz", 500_000_000L,
                "s3://backups/orders.sql.gz", false, 0, "0 bytes",
                "orders_production", null);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("FAILED", result.getOutputData().get("status"));
        assertEquals(false, result.getOutputData().get("verified"));

        String message = (String) result.getOutputData().get("message");
        assertTrue(message.contains("FAILED"));
    }

    @Test
    void recipientsPassedThrough() {
        List<String> recipients = List.of("alice@example.com", "bob@example.com");
        Task task = taskWith("file.sql.gz", 100L, "s3://b/f", true, 0, "0 bytes",
                "mydb", Map.of("channel", "email", "recipients", recipients));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> outputRecipients = (List<String>) result.getOutputData().get("recipients");
        assertEquals(2, outputRecipients.size());
        assertTrue(outputRecipients.contains("alice@example.com"));
        assertTrue(outputRecipients.contains("bob@example.com"));
    }

    @Test
    void defaultChannelIsConsole() {
        Task task = taskWith("file.sql.gz", 100L, "s3://b/f", true, 0, "0 bytes",
                "mydb", null);

        TaskResult result = worker.execute(task);

        assertEquals("console", result.getOutputData().get("channel"));
    }

    @Test
    void cleanupDetailsIncludedInMessage() {
        Task task = taskWith("file.sql.gz", 100L, "s3://b/f", true, 5, "2.3 GB",
                "mydb", null);

        TaskResult result = worker.execute(task);

        String message = (String) result.getOutputData().get("message");
        assertTrue(message.contains("5"));
        assertTrue(message.contains("2.3 GB"));
    }

    @Test
    void databaseNamePassedThrough() {
        Task task = taskWith("file.sql.gz", 100L, "s3://b/f", true, 0, "0 bytes",
                "analytics_db", null);

        TaskResult result = worker.execute(task);

        assertEquals("analytics_db", result.getOutputData().get("databaseName"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith("file.sql.gz", 100L, "s3://b/f", true, 0, "0 bytes",
                "mydb", Map.of("channel", "slack"));

        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("notificationSent"));
        assertNotNull(result.getOutputData().get("channel"));
        assertNotNull(result.getOutputData().get("recipients"));
        assertNotNull(result.getOutputData().get("status"));
        assertNotNull(result.getOutputData().get("message"));
        assertNotNull(result.getOutputData().get("databaseName"));
        assertNotNull(result.getOutputData().get("filename"));
        assertNotNull(result.getOutputData().get("verified"));
    }

    private Task taskWith(String filename, long sizeBytes, String storageUri, boolean verified,
                          int deletedCount, String freedFormatted, String databaseName,
                          Map<String, Object> notification) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("filename", filename);
        input.put("sizeBytes", sizeBytes);
        input.put("storageUri", storageUri);
        input.put("verified", verified);
        input.put("deletedCount", deletedCount);
        input.put("freedFormatted", freedFormatted);
        input.put("databaseName", databaseName);
        if (notification != null) {
            input.put("notification", notification);
        }
        task.setInputData(input);
        return task;
    }
}
