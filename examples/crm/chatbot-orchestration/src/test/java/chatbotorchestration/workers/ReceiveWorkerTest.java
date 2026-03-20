package chatbotorchestration.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ReceiveWorkerTest {
    private final ReceiveWorker worker = new ReceiveWorker();
    @Test void taskDefName() { assertEquals("cbo_receive", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("userId", "U1", "message", "refund", "sessionId", "S1", "intent", "request_refund", "entities", Map.of(), "response", "ok")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
