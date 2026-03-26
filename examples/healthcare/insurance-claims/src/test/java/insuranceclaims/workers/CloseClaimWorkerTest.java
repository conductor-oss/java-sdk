package insuranceclaims.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CloseClaimWorkerTest {
    private final CloseClaimWorker w = new CloseClaimWorker();
    @Test void taskDefName() { assertEquals("clm_close", w.getTaskDefName()); }
    @Test void closesClaim() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("claimId", "C1", "paidAmount", 800.0)));
        TaskResult r = w.execute(t);
        assertEquals("closed", r.getOutputData().get("claimStatus"));
    }
}
