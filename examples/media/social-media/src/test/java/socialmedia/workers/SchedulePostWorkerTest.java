package socialmedia.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class SchedulePostWorkerTest {
    private final SchedulePostWorker worker = new SchedulePostWorker();
    @Test void taskDefName() { assertEquals("soc_schedule_post", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1", "optimalTime", "2026-03-08T14:00:00Z")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("scheduledTime"));
        assertEquals("SCH-515-001", r.getOutputData().get("scheduleId"));
        assertEquals("America/New_York", r.getOutputData().get("timezone"));
    }
}
