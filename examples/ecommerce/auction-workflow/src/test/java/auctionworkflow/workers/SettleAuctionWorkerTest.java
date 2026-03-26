package auctionworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SettleAuctionWorkerTest {
    private final SettleAuctionWorker w = new SettleAuctionWorker();
    @Test void taskDefName() { assertEquals("auc_settle", w.getTaskDefName()); }
    @Test void settlesAuction() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("auctionId", "A1", "winnerId", "B2", "winningBid", 250)));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("settled"));
        assertNotNull(r.getOutputData().get("transactionId"));
    }
}
