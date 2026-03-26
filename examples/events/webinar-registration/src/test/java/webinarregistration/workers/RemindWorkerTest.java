package webinarregistration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RemindWorkerTest {
    private final RemindWorker worker = new RemindWorker();
    @Test void taskDefName() { assertEquals("wbr_remind", worker.getTaskDefName()); }
    @Test void sendsReminders() {
        TaskResult r = worker.execute(taskWith(Map.of("registrationId", "REG-1", "email", "a@b.com")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("reminders"));
        assertEquals(2, ((List<?>) r.getOutputData().get("reminders")).size());
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
