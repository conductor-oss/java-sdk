package interviewscheduling.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class RemindWorkerTest {
    private final RemindWorker w = new RemindWorker();
    @Test void taskDefName() { assertEquals("ivs_remind", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("interviewers","3","candidateName","Alex","interviewId","INT-603","scheduledTime","2024-03-25 10:00","slots","slot1")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
