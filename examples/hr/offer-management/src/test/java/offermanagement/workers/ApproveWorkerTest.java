package offermanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ApproveWorkerTest {
    private final ApproveWorker w = new ApproveWorker();
    @Test void taskDefName() { assertEquals("ofm_approve", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("candidateName","Alex","offerId","OFR-604","position","SWE","salary","155000")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
