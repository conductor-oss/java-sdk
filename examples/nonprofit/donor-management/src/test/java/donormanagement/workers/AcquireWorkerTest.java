package donormanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AcquireWorkerTest {
    @Test void testExecute() { AcquireWorker w = new AcquireWorker(); assertEquals("dnr_acquire", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("donorName", "Test", "donorEmail", "test@test.com"));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus()); }
}
