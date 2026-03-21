package gcpintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GcpGcsUploadWorkerTest {

    private final GcpGcsUploadWorker worker = new GcpGcsUploadWorker();

    @Test
    void taskDefName() {
        assertEquals("gcp_gcs_upload", worker.getTaskDefName());
    }

    @Test
    void uploadsObjectToGcs() {
        Task task = taskWith(Map.of(
                "bucket", "my-gcp-bucket",
                "objectName", "events/evt-6001.json",
                "data", Map.of("id", "evt-6001")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("gs://my-gcp-bucket/events/evt-6001.json", result.getOutputData().get("objectPath"));
    }

    @Test
    void outputContainsGeneration() {
        Task task = taskWith(Map.of("bucket", "b", "objectName", "o.json"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("generation"));
    }

    @Test
    void outputContainsSize() {
        Task task = taskWith(Map.of("bucket", "b", "objectName", "o.json"));
        TaskResult result = worker.execute(task);

        assertEquals(1024, result.getOutputData().get("size"));
    }

    @Test
    void handlesNullBucket() {
        Map<String, Object> input = new HashMap<>();
        input.put("bucket", null);
        input.put("objectName", "test.json");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(((String) result.getOutputData().get("objectPath")).startsWith("gs://default-bucket/"));
    }

    @Test
    void handlesNullObjectName() {
        Map<String, Object> input = new HashMap<>();
        input.put("bucket", "my-bucket");
        input.put("objectName", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("gs://my-bucket/events/unknown.json", result.getOutputData().get("objectPath"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("gs://default-bucket/events/unknown.json", result.getOutputData().get("objectPath"));
    }

    @Test
    void objectPathIsDeterministic() {
        Task task1 = taskWith(Map.of("bucket", "b1", "objectName", "f1.json"));
        Task task2 = taskWith(Map.of("bucket", "b1", "objectName", "f1.json"));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("objectPath"), r2.getOutputData().get("objectPath"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
