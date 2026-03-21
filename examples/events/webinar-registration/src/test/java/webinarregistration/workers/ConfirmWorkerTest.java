package webinarregistration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ConfirmWorkerTest {
    private final ConfirmWorker worker = new ConfirmWorker();
    @Test void taskDefName() { assertEquals("wbr_confirm", worker.getTaskDefName()); }
    @Test void confirmsRegistration() {
        TaskResult r = worker.execute(taskWith(Map.of("registrationId", "REG-1", "email", "a@b.com")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("confirmed"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
