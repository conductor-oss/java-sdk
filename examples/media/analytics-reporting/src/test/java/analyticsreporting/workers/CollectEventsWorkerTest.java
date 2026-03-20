package analyticsreporting.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CollectEventsWorkerTest {
    private final CollectEventsWorker worker = new CollectEventsWorker();
    @Test void taskDefName() { assertEquals("anr_collect_events", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(1250000, r.getOutputData().get("eventCount"));
        assertEquals("s3://analytics/raw/526/events.parquet", r.getOutputData().get("rawDataPath"));
        assertNotNull(r.getOutputData().get("sourcesProcessed"));
    }
}
