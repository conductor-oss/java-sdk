package voicebot.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class GenerateWorkerTest {
    private final GenerateWorker worker = new GenerateWorker();
    @Test void taskDefName() { assertEquals("vb_generate", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("audioUrl", "test.wav", "language", "en-US", "transcript", "hello", "intent", "order_status", "entities", Map.of("orderNumber", "1234"), "text", "response")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
