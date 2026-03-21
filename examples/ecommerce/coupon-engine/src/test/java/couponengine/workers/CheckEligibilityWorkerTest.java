package couponengine.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CheckEligibilityWorkerTest {
    private final CheckEligibilityWorker worker = new CheckEligibilityWorker();
    @Test void taskDefName() { assertEquals("cpn_check_eligibility", worker.getTaskDefName()); }
    @Test void eligibleWhenMeetsMinimum() {
        Task task = taskWith(Map.of("couponCode", "TEST", "customerId", "c1", "cartTotal", 100.0,
                "cartItems", List.of(), "couponRules", Map.of("minCartTotal", 50, "maxUses", 100, "currentUses", 42)));
        TaskResult r = worker.execute(task);
        assertEquals(true, r.getOutputData().get("eligible"));
    }
    @Test void notEligibleBelowMinimum() {
        Task task = taskWith(Map.of("couponCode", "TEST", "customerId", "c1", "cartTotal", 10.0,
                "cartItems", List.of(), "couponRules", Map.of("minCartTotal", 50, "maxUses", 100, "currentUses", 0)));
        TaskResult r = worker.execute(task);
        assertEquals(false, r.getOutputData().get("eligible"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
