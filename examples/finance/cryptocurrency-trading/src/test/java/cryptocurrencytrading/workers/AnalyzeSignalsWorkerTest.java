package cryptocurrencytrading.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AnalyzeSignalsWorkerTest {
    private final AnalyzeSignalsWorker worker = new AnalyzeSignalsWorker();
    @Test void taskDefName() { assertEquals("cry_analyze_signals", worker.getTaskDefName()); }
    @Test void analyzesSignals() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("pair","BTC/USD","currentPrice",67432.50)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("buy", r.getOutputData().get("signal"));
    }
}
