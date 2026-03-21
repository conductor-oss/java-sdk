package dependencymapping.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DiscoverServicesWorkerTest {
    @Test void taskDefName() { assertEquals("dep_discover_services", new DiscoverServicesWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("environment","production", "namespace","ecommerce")));
        TaskResult r = new DiscoverServicesWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(8, r.getOutputData().get("serviceCount"));
    }
}
