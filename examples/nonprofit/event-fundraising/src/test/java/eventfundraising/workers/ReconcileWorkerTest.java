package eventfundraising.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ReconcileWorkerTest {
    @Test void testExecute() { ReconcileWorker w = new ReconcileWorker(); assertEquals("efr_reconcile", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("eventId", "EVT-759", "revenue", 33000, "expenses", 12000));
        TaskResult r = w.execute(t); assertNotNull(r.getOutputData().get("fundraiser")); }
}
