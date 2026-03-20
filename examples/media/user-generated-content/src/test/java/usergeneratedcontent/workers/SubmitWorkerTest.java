package usergeneratedcontent.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class SubmitWorkerTest {
    private final SubmitWorker worker = new SubmitWorker();
    @Test void taskDefName() { assertEquals("ugc_submit", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("2026-03-08T09:00:00Z", r.getOutputData().get("receivedAt"));
        assertEquals(3, r.getOutputData().get("queuePosition"));
    }
}
