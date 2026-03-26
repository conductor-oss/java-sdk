package rootcauseanalysis.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RcaAnalyzeWorkerTest {
    @Test void taskDefName() { assertEquals("rca_analyze", new RcaAnalyzeWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("logs",2400, "metrics",150)));
        TaskResult r = new RcaAnalyzeWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(92, r.getOutputData().get("confidence"));
    }
}
