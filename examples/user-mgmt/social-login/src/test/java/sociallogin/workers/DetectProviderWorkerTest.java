package sociallogin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DetectProviderWorkerTest {
    private final DetectProviderWorker worker = new DetectProviderWorker();
    @Test void taskDefName() { assertEquals("slo_detect_provider", worker.getTaskDefName()); }
    @Test void detectsGoogle() {
        TaskResult r = worker.execute(taskWith(Map.of("provider", "google")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("google", r.getOutputData().get("providerName"));
        assertEquals(true, r.getOutputData().get("supported"));
    }
    @Test void includesEndpoint() {
        TaskResult r = worker.execute(taskWith(Map.of("provider", "github")));
        assertTrue(r.getOutputData().get("authEndpoint").toString().contains("github"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
