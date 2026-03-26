package analyticsreporting.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AggregateDataWorkerTest {
    private final AggregateDataWorker worker = new AggregateDataWorker();
    @Test void taskDefName() { assertEquals("anr_aggregate_data", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("aggregatedData"));
        assertEquals(85000, r.getOutputData().get("totalSessions"));
        assertEquals(42000, r.getOutputData().get("uniqueUsers"));
    }
}
