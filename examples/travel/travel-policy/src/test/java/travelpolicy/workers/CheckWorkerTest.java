package travelpolicy.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CheckWorkerTest {
    private final CheckWorker w = new CheckWorker();
    @Test void taskDefName() { assertEquals("tpl_check", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("employeeId","EMP-1000","bookingType","hotel","amount",300,"policyTier","standard")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
