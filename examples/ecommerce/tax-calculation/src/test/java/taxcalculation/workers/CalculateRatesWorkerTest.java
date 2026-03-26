package taxcalculation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CalculateRatesWorkerTest {
    private final CalculateRatesWorker w = new CalculateRatesWorker();
    @Test void taskDefName() { assertEquals("tax_calculate_rates", w.getTaskDefName()); }
    @Test void returnsCombinedRate() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("jurisdiction", Map.of("state", "CA"), "subtotal", 100.0)));
        TaskResult r = w.execute(t);
        assertEquals(0.075, ((Number) r.getOutputData().get("combinedRate")).doubleValue(), 0.001);
    }
}
