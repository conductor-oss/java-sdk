package taxcalculation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ApplyTaxWorkerTest {
    private final ApplyTaxWorker w = new ApplyTaxWorker();
    @Test void taskDefName() { assertEquals("tax_apply", w.getTaskDefName()); }
    @Test void appliesTax() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("orderId", "O1", "subtotal", 100.0, "taxRate", 0.085)));
        TaskResult r = w.execute(t);
        assertEquals(8.5, ((Number) r.getOutputData().get("taxAmount")).doubleValue());
        assertEquals(108.5, ((Number) r.getOutputData().get("total")).doubleValue());
    }
}
