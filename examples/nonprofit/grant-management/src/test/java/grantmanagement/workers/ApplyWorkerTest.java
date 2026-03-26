package grantmanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ApplyWorkerTest {
    @Test void testExecute() {
        ApplyWorker w = new ApplyWorker(); assertEquals("gmt_apply", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("organization", "Test", "program", "Test"));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
