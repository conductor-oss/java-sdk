package scholarshipprocessing.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NotifyWorkerTest {
    @Test void taskDefName() { assertEquals("scp_notify", new NotifyWorker().getTaskDefName()); }
    @Test void notifiesAwarded() {
        Task task = taskWith(Map.of("studentId", "STU-001", "awarded", true, "amount", 10000));
        TaskResult result = new NotifyWorker().execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("notified"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
