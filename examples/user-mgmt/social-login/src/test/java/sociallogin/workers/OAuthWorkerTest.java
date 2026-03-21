package sociallogin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OAuthWorkerTest {
    private final OAuthWorker worker = new OAuthWorker();
    @Test void taskDefName() { assertEquals("slo_auth", worker.getTaskDefName()); }
    @Test void validatesToken() {
        TaskResult r = worker.execute(taskWith(Map.of("provider", "google", "oauthToken", "tok")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("providerUserId"));
    }
    @Test void returnsProfile() {
        TaskResult r = worker.execute(taskWith(Map.of("provider", "google", "oauthToken", "tok")));
        assertNotNull(r.getOutputData().get("profile"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
