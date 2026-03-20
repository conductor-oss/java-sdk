package predictivemaintenance.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CollectDataWorkerTest {
    private final CollectDataWorker worker = new CollectDataWorker();
    @Test void taskDefName() { assertEquals("pmn_collect_data", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("operationalData"));
        assertEquals(178, r.getOutputData().get("currentTemp"));
        assertNotNull(r.getOutputData().get("vibrationLevel"));
    }
}
