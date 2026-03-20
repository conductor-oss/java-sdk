package impactreporting.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class PublishWorkerTest {
    @Test void testExecute() { PublishWorker w = new PublishWorker(); assertEquals("ipr_publish", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of());
        assertNotNull(w.execute(t).getOutputData().get("impact")); }
}
