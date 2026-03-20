package gcpintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GcpFirestoreWriteWorkerTest {

    private final GcpFirestoreWriteWorker worker = new GcpFirestoreWriteWorker();

    @Test
    void taskDefName() {
        assertEquals("gcp_firestore_write", worker.getTaskDefName());
    }

    @Test
    void writesDocumentWithId() {
        Task task = taskWith(Map.of(
                "collection", "events",
                "document", Map.of("id", "evt-6001", "type", "user.signup")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("evt-6001", result.getOutputData().get("documentId"));
        assertEquals("events", result.getOutputData().get("collection"));
    }

    @Test
    void outputContainsWriteTime() {
        Task task = taskWith(Map.of(
                "collection", "users",
                "document", Map.of("id", "u-1")));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("writeTime"));
    }

    @Test
    void handlesDocumentWithoutId() {
        Task task = taskWith(Map.of(
                "collection", "events",
                "document", Map.of("type", "order.created")));
        TaskResult result = worker.execute(task);

        assertEquals("doc-default", result.getOutputData().get("documentId"));
    }

    @Test
    void handlesNullCollection() {
        Map<String, Object> input = new HashMap<>();
        input.put("collection", null);
        input.put("document", Map.of("id", "d-1"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("default-collection", result.getOutputData().get("collection"));
    }

    @Test
    void handlesNullDocument() {
        Map<String, Object> input = new HashMap<>();
        input.put("collection", "events");
        input.put("document", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("doc-default", result.getOutputData().get("documentId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default-collection", result.getOutputData().get("collection"));
    }

    @Test
    void handlesNumericId() {
        Task task = taskWith(Map.of(
                "collection", "items",
                "document", Map.of("id", 99)));
        TaskResult result = worker.execute(task);

        assertEquals("99", result.getOutputData().get("documentId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
