package plagiarismdetection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ScanWorkerTest {
    private final ScanWorker worker = new ScanWorker();
    @Test void taskDefName() { assertEquals("plg_scan", worker.getTaskDefName()); }
    @Test void scansDocument() {
        Task task = taskWith(Map.of("documentText", "Hello world test"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("scanResults"));
    }
    @Test void handlesEmptyText() {
        Task task = taskWith(Map.of("documentText", ""));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
