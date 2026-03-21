package healthdashboard.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RenderDashboardWorkerTest {
    @Test void taskDefName() { assertEquals("hd_render_dashboard", new RenderDashboardWorker().getTaskDefName()); }
    @Test void allHealthy() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("apiStatus","healthy","dbStatus","healthy","cacheStatus","healthy","environment","prod")));
        TaskResult r = new RenderDashboardWorker().execute(t);
        assertEquals("GREEN", r.getOutputData().get("overallHealth"));
    }
    @Test void degraded() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("apiStatus","healthy","dbStatus","degraded","cacheStatus","healthy","environment","prod")));
        TaskResult r = new RenderDashboardWorker().execute(t);
        assertEquals("DEGRADED", r.getOutputData().get("overallHealth"));
    }
}
