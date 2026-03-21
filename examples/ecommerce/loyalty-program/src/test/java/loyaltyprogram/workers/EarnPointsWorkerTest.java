package loyaltyprogram.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EarnPointsWorkerTest {
    private final EarnPointsWorker w = new EarnPointsWorker();
    @Test void taskDefName() { assertEquals("loy_earn_points", w.getTaskDefName()); }
    @Test void goldGetsDoublePoints() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("customerId", "C1", "purchaseAmount", 100.0, "currentTier", "Gold")));
        TaskResult r = w.execute(t);
        assertEquals(200, ((Number) r.getOutputData().get("pointsEarned")).intValue());
        assertEquals(2, ((Number) r.getOutputData().get("multiplier")).intValue());
    }
    @Test void silverGetsSinglePoints() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("customerId", "C2", "purchaseAmount", 100.0, "currentTier", "Silver")));
        TaskResult r = w.execute(t);
        assertEquals(100, ((Number) r.getOutputData().get("pointsEarned")).intValue());
    }
}
