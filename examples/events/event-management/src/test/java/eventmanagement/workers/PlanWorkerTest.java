package eventmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PlanWorkerTest {
    private final PlanWorker worker = new PlanWorker();
    @Test void taskDefName() { assertEquals("evt_plan", worker.getTaskDefName()); }
    @Test void plansEvent() {
        TaskResult r = worker.execute(taskWith(Map.of("eventName", "Test", "date", "2024-01-01", "capacity", 100)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("eventId"));
        assertEquals("Convention Center Hall A", r.getOutputData().get("venue"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
