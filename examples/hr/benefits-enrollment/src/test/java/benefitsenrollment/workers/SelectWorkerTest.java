package benefitsenrollment.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class SelectWorkerTest {
    private final SelectWorker worker = new SelectWorker();
    @Test void taskDefName() { assertEquals("ben_select", worker.getTaskDefName()); }
    @Test void executes() {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("employeeId","EMP-600","enrollmentId","BEN-607","options","{}","selections","{}","validSelections","{}")));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
