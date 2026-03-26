package contentmoderation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class SubmitContentWorkerTest {
    private final SubmitContentWorker worker = new SubmitContentWorker();
    @Test void taskDefName() { assertEquals("mod_submit_content", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("2026-03-08T10:00:00Z", r.getOutputData().get("receivedAt"));
        assertEquals(1, r.getOutputData().get("queuePosition"));
    }
}
