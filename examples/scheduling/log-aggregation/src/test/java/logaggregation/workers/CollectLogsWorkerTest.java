package logaggregation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CollectLogsWorkerTest {
    @Test void taskDefName() { assertEquals("la_collect_logs", new CollectLogsWorker().getTaskDefName()); }
    @Test void collectsLogs() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("timeRange","last-1h","logLevel","INFO")));
        TaskResult r = new CollectLogsWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(15000, r.getOutputData().get("rawLogCount"));
    }
}
