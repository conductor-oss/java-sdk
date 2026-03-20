package logaggregation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class StoreLogsWorkerTest {
    @Test void taskDefName() { assertEquals("la_store_logs", new StoreLogsWorker().getTaskDefName()); }
    @Test void storesLogs() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("enrichedCount",14800,"sizeBytes",45000000)));
        TaskResult r = new StoreLogsWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("stored"));
    }
}
