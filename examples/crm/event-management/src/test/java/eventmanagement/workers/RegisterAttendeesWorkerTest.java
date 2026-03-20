package eventmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RegisterAttendeesWorkerTest {
    private final RegisterAttendeesWorker worker = new RegisterAttendeesWorker();
    @Test void taskDefName() { assertEquals("evt_register", worker.getTaskDefName()); }
    @Test void registersAttendees() {
        TaskResult r = worker.execute(taskWith(Map.of("eventId", "EVT-1", "capacity", 200)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(187, r.getOutputData().get("registeredCount"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
