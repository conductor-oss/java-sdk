package expensemanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ReimburseWorkerTest {
    private final ReimburseWorker worker = new ReimburseWorker();
    @Test void taskDefName() { assertEquals("exp_reimburse", worker.getTaskDefName()); }
    @Test void processesReimbursement() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("approvedAmount", 342.50, "employeeId", "EMP-1")));
        TaskResult r = worker.execute(t);
        assertEquals("processed", r.getOutputData().get("reimbursementStatus"));
    }
}
