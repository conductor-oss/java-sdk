package loyaltyprogram.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RewardWorkerTest {
    private final RewardWorker w = new RewardWorker();
    @Test void taskDefName() { assertEquals("loy_reward", w.getTaskDefName()); }
    @Test void goldReward() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("customerId", "C1", "tier", "Gold", "pointsEarned", 200)));
        TaskResult r = w.execute(t);
        assertEquals("15% off + free shipping + early access", r.getOutputData().get("reward"));
    }
}
