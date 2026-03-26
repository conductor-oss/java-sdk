package auctionworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DetermineWinnerWorkerTest {
    private final DetermineWinnerWorker w = new DetermineWinnerWorker();
    @Test void taskDefName() { assertEquals("auc_determine_winner", w.getTaskDefName()); }
    @Test void findsHighestBidder() {
        List<Map<String, Object>> bids = List.of(Map.of("bidderId", "B1", "amount", 100), Map.of("bidderId", "B2", "amount", 250), Map.of("bidderId", "B3", "amount", 200));
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("auctionId", "A1", "bids", bids)));
        TaskResult r = w.execute(t);
        assertEquals("B2", r.getOutputData().get("winnerId"));
        assertEquals(250, ((Number) r.getOutputData().get("winningBid")).intValue());
    }
}
