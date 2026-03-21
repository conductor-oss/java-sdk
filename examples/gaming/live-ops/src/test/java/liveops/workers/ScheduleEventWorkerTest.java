package liveops.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ScheduleEventWorkerTest {
    @Test void testExecute() {
        ScheduleEventWorker w = new ScheduleEventWorker();
        assertEquals("lop_schedule_event", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("eventName", "Test"));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
