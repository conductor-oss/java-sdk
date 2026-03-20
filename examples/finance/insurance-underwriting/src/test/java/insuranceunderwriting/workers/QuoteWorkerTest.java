package insuranceunderwriting.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class QuoteWorkerTest {
    private final QuoteWorker worker = new QuoteWorker();
    @Test void taskDefName() { assertEquals("uw_quote", worker.getTaskDefName()); }
    @Test void preferredClassDiscount() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("coverageAmount", 500000, "riskClass", "preferred")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        double premium = ((Number) r.getOutputData().get("premium")).doubleValue();
        assertEquals(600.0, premium, 0.01);
    }
    @Test void substandardClassSurcharge() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("coverageAmount", 500000, "riskClass", "substandard")));
        TaskResult r = worker.execute(t);
        double premium = ((Number) r.getOutputData().get("premium")).doubleValue();
        assertEquals(1125.0, premium, 0.01);
    }
}
