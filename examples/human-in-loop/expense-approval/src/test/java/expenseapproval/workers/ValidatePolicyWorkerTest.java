package expenseapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ValidatePolicyWorkerTest {
    @Test void taskDefName() { assertEquals("exp_validate_policy", new ValidatePolicyWorker().getTaskDefName()); }

    @Test void highAmountNeedsApproval() {
        ValidatePolicyWorker w = new ValidatePolicyWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("amount", 500, "category", "office")));
        TaskResult r = w.execute(t);
        assertEquals("true", r.getOutputData().get("approvalRequired"));
    }

    @Test void lowAmountAutoApproved() {
        ValidatePolicyWorker w = new ValidatePolicyWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("amount", 50, "category", "office")));
        TaskResult r = w.execute(t);
        assertEquals("false", r.getOutputData().get("approvalRequired"));
    }

    @Test void travelAlwaysNeedsApproval() {
        ValidatePolicyWorker w = new ValidatePolicyWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("amount", 25, "category", "travel")));
        TaskResult r = w.execute(t);
        assertEquals("true", r.getOutputData().get("approvalRequired"));
    }
}
