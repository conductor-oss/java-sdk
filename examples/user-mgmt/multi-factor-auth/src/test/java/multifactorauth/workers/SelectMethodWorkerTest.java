package multifactorauth.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SelectMethodWorkerTest {
    private final SelectMethodWorker worker = new SelectMethodWorker();

    @Test void taskDefName() { assertEquals("mfa_select_method", worker.getTaskDefName()); }
    @Test void selectsPreferredMethod() {
        Task task = taskWith(Map.of("userId", "USR-123", "preferred", "sms"));
        TaskResult r = worker.execute(task);
        assertEquals("sms", r.getOutputData().get("selectedMethod"));
    }
    @Test void defaultsToTotp() {
        Map<String, Object> input = new HashMap<>(); input.put("userId", "USR-123"); input.put("preferred", null);
        Task task = taskWith(input);
        TaskResult r = worker.execute(task);
        assertEquals("totp", r.getOutputData().get("selectedMethod"));
    }
    @Test void includesAvailableMethods() {
        Task task = taskWith(Map.of("userId", "USR-123", "preferred", "totp"));
        TaskResult r = worker.execute(task);
        assertNotNull(r.getOutputData().get("available"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
