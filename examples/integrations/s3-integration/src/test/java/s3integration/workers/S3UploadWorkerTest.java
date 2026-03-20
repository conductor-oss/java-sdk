package s3integration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class S3UploadWorkerTest {

    private final S3UploadWorker worker = new S3UploadWorker();

    @Test
    void taskDefName() {
        assertEquals("s3_upload", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("bucket", "my-bucket", "key", "file.pdf", "contentType", "application/pdf"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("etag"));
        assertEquals(2048, result.getOutputData().get("size"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
