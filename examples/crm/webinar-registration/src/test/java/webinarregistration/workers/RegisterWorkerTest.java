package webinarregistration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RegisterWorkerTest {
    private final RegisterWorker worker = new RegisterWorker();
    @Test void taskDefName() { assertEquals("wbr_register", worker.getTaskDefName()); }
    @Test void registersAttendee() {
        TaskResult r = worker.execute(taskWith(Map.of("name", "Alice", "email", "a@b.com", "webinarId", "WEB-1")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("registrationId"));
        assertNotNull(r.getOutputData().get("joinLink"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
