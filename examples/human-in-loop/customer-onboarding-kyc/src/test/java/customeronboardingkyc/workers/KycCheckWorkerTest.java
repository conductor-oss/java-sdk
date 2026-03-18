package customeronboardingkyc.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class KycCheckWorkerTest {
    @Test void taskDefName() { assertEquals("kyc_check", new KycCheckWorker().getTaskDefName()); }

    @Test void highRiskNeedsReview() {
        KycCheckWorker w = new KycCheckWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("customerId", "C-1", "customerName", "Test", "riskLevel", "high")));
        TaskResult r = w.execute(t);
        assertEquals("true", r.getOutputData().get("needsReview"));
    }

    @Test void lowRiskAutoApproved() {
        KycCheckWorker w = new KycCheckWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("customerId", "C-2", "customerName", "Test", "riskLevel", "low")));
        TaskResult r = w.execute(t);
        assertEquals("false", r.getOutputData().get("needsReview"));
    }
}
