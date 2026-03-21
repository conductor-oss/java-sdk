package insuranceclaims.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class AdjudicateClaimWorkerTest {
    private final AdjudicateClaimWorker w = new AdjudicateClaimWorker();
    @Test void taskDefName() { assertEquals("clm_adjudicate", w.getTaskDefName()); }
    @Test void calculatesApproved() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("claimId", "C1", "amount", 1000.0, "eligible", true, "coverage", 80)));
        TaskResult r = w.execute(t);
        assertEquals(800.0, ((Number) r.getOutputData().get("approvedAmount")).doubleValue());
    }
}
