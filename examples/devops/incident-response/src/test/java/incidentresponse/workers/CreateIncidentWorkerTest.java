package incidentresponse.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreateIncidentWorkerTest {

    private final CreateIncidentWorker worker = new CreateIncidentWorker();

    @Test
    void taskDefName() {
        assertEquals("ir_create_incident", worker.getTaskDefName());
    }

    @Test
    void createsIncidentWithUniqueId() {
        Task task = taskWith(Map.of("severity", "P1", "alertName", "High CPU"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String incidentId = (String) result.getOutputData().get("incidentId");
        assertNotNull(incidentId);
        assertTrue(incidentId.startsWith("INC-"));
    }

    @Test
    void createsRealJsonFile() {
        Task task = taskWith(Map.of("severity", "P1", "alertName", "Test Alert"));
        TaskResult result = worker.execute(task);

        String filePath = (String) result.getOutputData().get("incidentFile");
        assertNotNull(filePath);
        assertTrue(Files.exists(Path.of(filePath)), "Incident file should exist on disk");
    }

    @Test
    void incidentFileContainsAlertDetails() throws Exception {
        Task task = taskWith(Map.of("severity", "P2", "alertName", "Memory Leak"));
        TaskResult result = worker.execute(task);

        String filePath = (String) result.getOutputData().get("incidentFile");
        String content = Files.readString(Path.of(filePath));

        assertTrue(content.contains("Memory Leak"));
        assertTrue(content.contains("P2"));
        assertTrue(content.contains("open"));
        assertTrue(content.contains(result.getOutputData().get("incidentId").toString()));
    }

    @Test
    void eachCallGeneratesUniqueId() {
        Task task1 = taskWith(Map.of("severity", "P1"));
        Task task2 = taskWith(Map.of("severity", "P1"));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertNotEquals(r1.getOutputData().get("incidentId"), r2.getOutputData().get("incidentId"));
    }

    @Test
    void handlesDifferentSeverity() {
        Task task = taskWith(Map.of("severity", "P3"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("P3", result.getOutputData().get("severity"));
    }

    @Test
    void handlesNullSeverity() {
        Map<String, Object> input = new HashMap<>();
        input.put("severity", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("incidentId"));
    }

    @Test
    void outputContainsCreatedAt() {
        Task task = taskWith(Map.of("severity", "P1"));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("createdAt"));
    }

    @Test
    void outputStatusIsOpen() {
        Task task = taskWith(Map.of("severity", "P1"));
        TaskResult result = worker.execute(task);
        assertEquals("open", result.getOutputData().get("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
