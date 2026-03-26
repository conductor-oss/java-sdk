package contentarchival.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CompressWorkerTest {
    private final CompressWorker worker = new CompressWorker();
    @Test void taskDefName() { assertEquals("car_compress", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("s3://staging/archive/528/compressed.tar.zst", r.getOutputData().get("compressedPath"));
        assertNotNull(r.getOutputData().get("compressedSizeMb"));
        assertNotNull(r.getOutputData().get("compressionRatio"));
    }
}
