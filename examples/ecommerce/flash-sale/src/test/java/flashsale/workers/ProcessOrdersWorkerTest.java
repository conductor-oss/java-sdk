package flashsale.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ProcessOrdersWorkerTest {
    private final ProcessOrdersWorker w = new ProcessOrdersWorker();
    @Test void taskDefName() { assertEquals("fls_process_orders", w.getTaskDefName()); }
    @Test void capsOrdersAtAvailableStock() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("saleId", "S1", "availableStock", 300)));
        TaskResult r = w.execute(t);
        assertEquals(275, ((Number) r.getOutputData().get("ordersFilled")).intValue());
        assertEquals(25, ((Number) r.getOutputData().get("remainingStock")).intValue());
    }
}
