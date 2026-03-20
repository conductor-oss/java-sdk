package taxfiling.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CalculateTaxWorkerTest {
    private final CalculateTaxWorker worker = new CalculateTaxWorker();
    @Test void taskDefName() { assertEquals("txf_calculate_tax", worker.getTaskDefName()); }
    @Test void calculatesCorrectly() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("grossIncome", 125000, "deductions", 27500, "credits", 4000)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        long liability = ((Number)r.getOutputData().get("taxLiability")).longValue();
        assertEquals(17450, liability);
    }
}
