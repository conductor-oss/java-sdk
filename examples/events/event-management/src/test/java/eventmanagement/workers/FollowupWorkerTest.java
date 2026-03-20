package eventmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FollowupWorkerTest {
    private final FollowupWorker worker = new FollowupWorker();
    @Test void taskDefName() { assertEquals("evt_followup", worker.getTaskDefName()); }
    @Test void sendsFollowup() {
        TaskResult r = worker.execute(taskWith(Map.of("eventId", "EVT-1", "attendees", 82)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("surveySent"));
        assertEquals(4.6, r.getOutputData().get("satisfaction"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
