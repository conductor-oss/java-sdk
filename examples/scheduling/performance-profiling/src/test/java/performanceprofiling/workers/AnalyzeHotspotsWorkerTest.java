package performanceprofiling.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AnalyzeHotspotsWorkerTest {
    @Test void taskDefName() { assertEquals("prf_analyze_hotspots", new AnalyzeHotspotsWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("sampleCount",45000, "profileType","cpu")));
        TaskResult r = new AnalyzeHotspotsWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(5, r.getOutputData().get("hotspotCount"));
    }
}
