package userfeedback.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RespondFeedbackWorkerTest {
    private final RespondFeedbackWorker w = new RespondFeedbackWorker();
    @Test void taskDefName() { assertEquals("ufb_respond", w.getTaskDefName()); }
    @Test void responds() {
        TaskResult r = w.execute(t(Map.of("userId", "USR-1", "category", "bug", "team", "engineering")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("responseSent"));
        assertTrue(r.getOutputData().get("message").toString().contains("bug"));
    }
    private Task t(Map<String, Object> i) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(i)); return t; }
}
