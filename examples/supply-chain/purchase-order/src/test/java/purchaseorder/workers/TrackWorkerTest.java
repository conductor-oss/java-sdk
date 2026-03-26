package purchaseorder.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TrackWorkerTest {
    private final TrackWorker worker = new TrackWorker();

    @Test void taskDefName() { assertEquals("po_track", worker.getTaskDefName()); }

    @Test void tracksOrder() {
        Task task = taskWith(Map.of("poNumber", "PO-001"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("confirmed", result.getOutputData().get("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
