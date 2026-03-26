package travelpolicy.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ProcessWorkerTest {
    private final ProcessWorker w = new ProcessWorker();
    @Test void taskDefName() { assertEquals("tpl_process", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("employeeId","EMP-1000","bookingType","hotel","amount","release-management","policyTier","standard","complianceResult","exception","reason","exceeds-limit")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
