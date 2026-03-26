package purchaseorder.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReceiveWorkerTest {
    private final ReceiveWorker worker = new ReceiveWorker();

    @Test void taskDefName() { assertEquals("po_receive", worker.getTaskDefName()); }

    @Test void receivesGoods() {
        Task task = taskWith(Map.of("poNumber", "PO-001", "trackingStatus", "confirmed"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("received"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
