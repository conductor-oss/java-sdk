package eventmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ExecuteEventWorkerTest {
    private final ExecuteEventWorker worker = new ExecuteEventWorker();
    @Test void taskDefName() { assertEquals("evt_execute", worker.getTaskDefName()); }
    @Test void executesEvent() {
        TaskResult r = worker.execute(taskWith(Map.of("eventId", "EVT-1", "attendees", 100)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(82, r.getOutputData().get("actualAttendees"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
