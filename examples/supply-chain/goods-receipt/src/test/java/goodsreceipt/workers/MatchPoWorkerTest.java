package goodsreceipt.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MatchPoWorkerTest {
    private final MatchPoWorker worker = new MatchPoWorker();

    @Test void taskDefName() { assertEquals("grc_match_po", worker.getTaskDefName()); }

    @Test void matchesPO() {
        Task task = taskWith(Map.of("poNumber", "PO-001", "receivedItems", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("matched"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
