package expensereporting.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class SubmitWorkerTest {
    private final SubmitWorker w = new SubmitWorker();
    @Test void taskDefName() { assertEquals("exr_submit", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("employeeId","EMP-900","tripId","TRIP-1","receipts","{}","categorized","{}","reportId","EXP-543","total","1500")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
