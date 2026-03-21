package imagepipeline.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class UploadImageWorkerTest {
    private final UploadImageWorker worker = new UploadImageWorker();
    @Test void taskDefName() { assertEquals("imp_upload_image", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("s3://media/images/513/original.jpg", r.getOutputData().get("storagePath"));
        assertEquals(4000, r.getOutputData().get("originalWidth"));
        assertEquals(3000, r.getOutputData().get("originalHeight"));
    }
}
