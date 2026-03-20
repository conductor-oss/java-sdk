package socialmedia.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CreateContentWorkerTest {
    private final CreateContentWorker worker = new CreateContentWorker();
    @Test void taskDefName() { assertEquals("soc_create_content", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1", "message", "Hello world")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("SOC-CNT-515-001", r.getOutputData().get("contentId"));
        assertNotNull(r.getOutputData().get("formattedMessage"));
        assertNotNull(r.getOutputData().get("hashtags"));
    }
}
