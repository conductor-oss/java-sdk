package loyaltyprogram.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CheckTierWorkerTest {
    private final CheckTierWorker w = new CheckTierWorker();
    @Test void taskDefName() { assertEquals("loy_check_tier", w.getTaskDefName()); }
    @Test void silverUpgradesToGold() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("customerId", "C1", "totalPoints", 5500, "currentTier", "Silver")));
        TaskResult r = w.execute(t);
        assertEquals("Gold", r.getOutputData().get("newTier"));
        assertEquals("true", r.getOutputData().get("upgradeEligible"));
    }
    @Test void noUpgradeWhenBelowThreshold() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("customerId", "C2", "totalPoints", 1500, "currentTier", "Bronze")));
        TaskResult r = w.execute(t);
        assertEquals("Bronze", r.getOutputData().get("newTier"));
        assertEquals("false", r.getOutputData().get("upgradeEligible"));
    }
}
