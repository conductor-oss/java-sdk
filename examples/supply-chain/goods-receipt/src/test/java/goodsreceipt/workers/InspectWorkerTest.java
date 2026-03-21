package goodsreceipt.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InspectWorkerTest {
    private final InspectWorker worker = new InspectWorker();

    @Test void taskDefName() { assertEquals("grc_inspect", worker.getTaskDefName()); }

    @Test void inspectsItems() {
        Task task = taskWith(Map.of("receivedItems", List.of(Map.of("sku", "A"))));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("passed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
