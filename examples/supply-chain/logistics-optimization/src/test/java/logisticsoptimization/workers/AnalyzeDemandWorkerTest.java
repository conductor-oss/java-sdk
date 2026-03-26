package logisticsoptimization.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AnalyzeDemandWorkerTest {
    @Test void taskDefName() { assertEquals("lo_analyze_demand", new AnalyzeDemandWorker().getTaskDefName()); }
    @Test void analyzesDemand() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("region","midwest","orders",List.of(Map.of("id","O1")))));
        TaskResult r = new AnalyzeDemandWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(1, r.getOutputData().get("orderCount"));
    }
}
