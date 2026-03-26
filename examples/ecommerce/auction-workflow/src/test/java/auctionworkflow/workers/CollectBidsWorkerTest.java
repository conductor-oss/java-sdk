package auctionworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CollectBidsWorkerTest {
    private final CollectBidsWorker w = new CollectBidsWorker();
    @Test void taskDefName() { assertEquals("auc_collect_bids", w.getTaskDefName()); }
    @Test void returnsBids() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("auctionId", "A1", "startingPrice", 200)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(5, ((Number) r.getOutputData().get("bidCount")).intValue());
        assertTrue(r.getOutputData().get("bids") instanceof List);
    }
}
