package dataexportrequest.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DeliverExportWorkerTest {
    private final DeliverExportWorker worker = new DeliverExportWorker();

    @Test void taskDefName() { assertEquals("der_deliver", worker.getTaskDefName()); }

    @Test void deliversExport() {
        Task task = taskWith(Map.of("userId", "USR-123", "packageUrl", "https://example.com/export.json"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("delivered"));
    }

    @Test void includesExpiry() {
        Task task = taskWith(Map.of("userId", "USR-123", "packageUrl", "https://example.com/export.json"));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("expiresAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
