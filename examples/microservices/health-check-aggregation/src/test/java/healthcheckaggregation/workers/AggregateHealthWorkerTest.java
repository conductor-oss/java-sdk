package healthcheckaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AggregateHealthWorkerTest {
    private final AggregateHealthWorker worker = new AggregateHealthWorker();
    @Test void taskDefName() { assertEquals("hc_aggregate_health", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("api", Map.of("healthy", true), "db", Map.of("healthy", true), "cache", Map.of("healthy", true), "queue", Map.of("healthy", true))));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("status"));
    }
}
