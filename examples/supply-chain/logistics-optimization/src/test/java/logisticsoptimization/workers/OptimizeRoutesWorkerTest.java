package logisticsoptimization.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OptimizeRoutesWorkerTest {
    @Test void taskDefName() { assertEquals("lo_optimize_routes", new OptimizeRoutesWorker().getTaskDefName()); }
    @Test void optimizesRoutes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("demandMap", Map.of())));
        TaskResult r = new OptimizeRoutesWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(3, r.getOutputData().get("routeCount"));
    }
}
