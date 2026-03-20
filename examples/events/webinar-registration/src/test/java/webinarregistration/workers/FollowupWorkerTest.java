package webinarregistration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FollowupWorkerTest {
    private final FollowupWorker worker = new FollowupWorker();
    @Test void taskDefName() { assertEquals("wbr_followup", worker.getTaskDefName()); }
    @Test void sendsFollowup() {
        TaskResult r = worker.execute(taskWith(Map.of("registrationId", "REG-1", "email", "a@b.com")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("sent"));
        assertNotNull(r.getOutputData().get("content"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
