package maintenancewindows.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CheckWindowWorkerTest {
    @Test void taskDefName() { assertEquals("mnw_check_window", new CheckWindowWorker().getTaskDefName()); }
    @Test void checksWindow() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("system", "prod-db", "currentTime", "2026-03-08T03:30:00Z")));
        TaskResult r = new CheckWindowWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("in_window", r.getOutputData().get("windowStatus"));
    }
}
