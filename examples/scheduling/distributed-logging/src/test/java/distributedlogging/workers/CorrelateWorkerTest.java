package distributedlogging.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CorrelateWorkerTest {
    @Test void taskDefName() { assertEquals("dg_correlate", new CorrelateWorker().getTaskDefName()); }
    @Test void correlatesLogs() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("svc1Logs",45,"svc2Logs",32,"svc3Logs",28,"traceId","trace-123")));
        TaskResult r = new CorrelateWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(105, r.getOutputData().get("totalLogs"));
        assertEquals(12, r.getOutputData().get("correlatedEvents"));
    }
}
