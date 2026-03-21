package expensemanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ApproveExpenseWorkerTest {
    private final ApproveExpenseWorker worker = new ApproveExpenseWorker();
    @Test void taskDefName() { assertEquals("exp_approve_expense", worker.getTaskDefName()); }
    @Test void approvesExpense() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("amount", 342.50, "employeeId", "EMP-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("approved"));
        assertEquals(342.50, ((Number)r.getOutputData().get("approvedAmount")).doubleValue());
    }
}
