package trainingmanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AssessWorkerTest {
    private final AssessWorker w = new AssessWorker();
    @Test void taskDefName() { assertEquals("trm_assess", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("employeeId","EMP-700","courseId","CRS-101","enrollmentId","ENR-700","score","92","certificationId","CERT-700")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
