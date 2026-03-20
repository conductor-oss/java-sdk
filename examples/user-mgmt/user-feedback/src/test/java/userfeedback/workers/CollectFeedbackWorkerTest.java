package userfeedback.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CollectFeedbackWorkerTest {
    private final CollectFeedbackWorker w = new CollectFeedbackWorker();
    @Test void taskDefName() { assertEquals("ufb_collect", w.getTaskDefName()); }
    @Test void collectsFeedback() {
        TaskResult r = w.execute(t(Map.of("userId", "USR-1", "feedbackText", "test", "source", "in-app")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("feedbackId").toString().startsWith("FB-"));
        assertNotNull(r.getOutputData().get("receivedAt"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
