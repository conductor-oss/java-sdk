package legalbilling.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GenerateWorkerTest {
    @Test void taskDefName() { assertEquals("lgb_generate", new GenerateWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("clientId", "C1", "matterId", "M1", "billingPeriod", "Q1", "timeEntries", "entries", "approvedEntries", "entries", "invoiceId", "I1")));
        TaskResult r = new GenerateWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("invoiceId"));
    }
}
