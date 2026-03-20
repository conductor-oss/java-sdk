package incidentai.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class DetectWorkerTest {
    private final DetectWorker worker = new DetectWorker();
    @Test void taskDefName() { assertEquals("iai_detect", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("alertId","ALT-1"); input.put("serviceName","checkout");
        t.setInputData(input);
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
