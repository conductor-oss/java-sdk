package customersegmentation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CollectDataWorkerTest {
    private final CollectDataWorker worker = new CollectDataWorker();
    @Test void taskDefName() { assertEquals("seg_collect_data", worker.getTaskDefName()); }
    @Test void returnsCustomers() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("datasetId", "DS-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("customers") instanceof List);
        assertEquals(4, ((Number) r.getOutputData().get("totalCustomers")).intValue());
    }
}
