package awsintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AwsS3UploadWorkerTest {

    private final AwsS3UploadWorker worker = new AwsS3UploadWorker();

    @Test
    void taskDefName() {
        assertEquals("aws_s3_upload", worker.getTaskDefName());
    }

    @Test
    void uploadsObjectWithIdFromBody() {
        Task task = taskWith(Map.of(
                "bucket", "my-data-bucket",
                "key", "data/evt-5001.json",
                "body", Map.of("id", "evt-5001", "type", "order.created")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("data/evt-5001.json", result.getOutputData().get("s3Key"));
        assertEquals("my-data-bucket", result.getOutputData().get("bucket"));
    }

    @Test
    void outputContainsEtag() {
        Task task = taskWith(Map.of(
                "bucket", "test-bucket",
                "body", Map.of("id", "evt-001")));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("etag"));
    }

    @Test
    void handlesBodyWithoutId() {
        Task task = taskWith(Map.of(
                "bucket", "test-bucket",
                "body", Map.of("type", "order.created")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("data/unknown.json", result.getOutputData().get("s3Key"));
    }

    @Test
    void handlesNullBucket() {
        Map<String, Object> input = new HashMap<>();
        input.put("bucket", null);
        input.put("body", Map.of("id", "evt-100"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default-bucket", result.getOutputData().get("bucket"));
    }

    @Test
    void handlesNullBody() {
        Map<String, Object> input = new HashMap<>();
        input.put("bucket", "my-bucket");
        input.put("body", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("data/unknown.json", result.getOutputData().get("s3Key"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default-bucket", result.getOutputData().get("bucket"));
        assertEquals("data/unknown.json", result.getOutputData().get("s3Key"));
    }

    @Test
    void s3KeyIncludesIdFromPayload() {
        Task task = taskWith(Map.of(
                "bucket", "reports",
                "body", Map.of("id", "report-42")));
        TaskResult result = worker.execute(task);

        assertEquals("data/report-42.json", result.getOutputData().get("s3Key"));
        assertEquals("reports", result.getOutputData().get("bucket"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
