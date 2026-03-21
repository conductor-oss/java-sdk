package volunteercoordination.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ThankWorkerTest {
    @Test void testExecute() { ThankWorker w = new ThankWorker(); assertEquals("vol_thank", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("volunteerName", "Test", "hours", 4));
        assertNotNull(w.execute(t).getOutputData().get("volunteer")); }
}
