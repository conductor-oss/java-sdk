package firmwareupdate.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CheckVersionWorkerTest {
    private final CheckVersionWorker worker = new CheckVersionWorker();
    @Test void taskDefName() { assertEquals("fw_check_version", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("updateAvailable"));
        assertEquals("https://firmware.example.com/v2.5.0/firmware.bin", r.getOutputData().get("downloadUrl"));
        assertEquals("sha256:abc123def456ghi789", r.getOutputData().get("checksum"));
    }
}
