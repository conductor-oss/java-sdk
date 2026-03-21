package couponengine.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RecordUsageWorkerTest {
    private final RecordUsageWorker worker = new RecordUsageWorker();
    @Test void taskDefName() { assertEquals("cpn_record_usage", worker.getTaskDefName()); }
    @Test void recordsUsage() {
        Task task = taskWith(Map.of("couponCode", "TEST", "customerId", "c1", "discountApplied", 20.0));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("recorded"));
        assertNotNull(r.getOutputData().get("usageId"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
