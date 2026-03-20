package flashsale.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ReportWorkerTest {
    private final ReportWorker w = new ReportWorker();
    @Test void taskDefName() { assertEquals("fls_report", w.getTaskDefName()); }
    @Test void calculatesSoldOutPercent() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("saleId", "S1", "saleName", "Sale", "ordersFilled", 275, "revenue", "15377.50", "remainingStock", 25)));
        TaskResult r = w.execute(t);
        assertEquals(92, ((Number) r.getOutputData().get("soldOutPercent")).intValue());
        assertEquals(true, r.getOutputData().get("reportGenerated"));
    }
}
