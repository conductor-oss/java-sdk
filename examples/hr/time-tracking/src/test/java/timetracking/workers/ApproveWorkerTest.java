package timetracking.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ApproveWorkerTest {
    private final ApproveWorker worker = new ApproveWorker();
    @Test void taskDefName() { assertEquals("ttk_approve", worker.getTaskDefName()); }
    @Test void executes() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("employeeId", "EMP-500", "timesheetId", "TS-606", "weekEnding", "2024-03-22", "entries", 5, "totalHours", 40)));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }
}
