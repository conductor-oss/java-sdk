package payrollworkflow.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ApplyDeductionsWorkerTest {
    private final ApplyDeductionsWorker worker = new ApplyDeductionsWorker();
    @Test void taskDefName() { assertEquals("prl_apply_deductions", worker.getTaskDefName()); }
    @Test void appliesDeductions() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("grossPayroll", 10000.0)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        double net = ((Number)r.getOutputData().get("netPayroll")).doubleValue();
        assertTrue(net > 0 && net < 10000);
    }
}
