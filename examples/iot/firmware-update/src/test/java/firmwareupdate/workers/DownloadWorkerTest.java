package firmwareupdate.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class DownloadWorkerTest {
    private final DownloadWorker worker = new DownloadWorker();
    @Test void taskDefName() { assertEquals("fw_download", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("/tmp/firmware/v2.5.0/firmware.bin", r.getOutputData().get("firmwarePath"));
        assertNotNull(r.getOutputData().get("downloadedSizeMb"));
        assertEquals(3200, r.getOutputData().get("downloadTimeMs"));
    }
}
