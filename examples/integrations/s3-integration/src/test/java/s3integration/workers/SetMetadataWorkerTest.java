package s3integration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SetMetadataWorkerTest {

    private final SetMetadataWorker worker = new SetMetadataWorker();

    @Test
    void taskDefName() {
        assertEquals("s3_set_metadata", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("bucket", "my-bucket", "key", "file.pdf", "etag", "\"abc\""));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("updated"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
