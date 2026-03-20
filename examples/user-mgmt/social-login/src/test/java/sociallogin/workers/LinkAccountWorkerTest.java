package sociallogin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LinkAccountWorkerTest {
    private final LinkAccountWorker worker = new LinkAccountWorker();
    @Test void taskDefName() { assertEquals("slo_link_account", worker.getTaskDefName()); }
    @Test void linksAccount() {
        TaskResult r = worker.execute(taskWith(Map.of("email", "a@b.com", "providerUserId", "prov_123", "provider", "google")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("userId").toString().startsWith("USR-"));
    }
    @Test void includesLinkedProviders() {
        TaskResult r = worker.execute(taskWith(Map.of("email", "a@b.com", "providerUserId", "prov_123", "provider", "github")));
        assertNotNull(r.getOutputData().get("linkedProviders"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
