package s3integration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class S3NotifyWorkerTest {

    private final S3NotifyWorker worker = new S3NotifyWorker();

    @Test
    void taskDefName() {
        assertEquals("s3_notify", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("email", "user@test.com", "presignedUrl", "https://example.com", "key", "file.pdf"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("notified"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
