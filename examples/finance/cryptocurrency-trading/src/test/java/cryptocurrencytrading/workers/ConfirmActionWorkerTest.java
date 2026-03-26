package cryptocurrencytrading.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ConfirmActionWorkerTest {
    private final ConfirmActionWorker worker = new ConfirmActionWorker();
    @Test void taskDefName() { assertEquals("cry_confirm_action", worker.getTaskDefName()); }
    @Test void confirmsAction() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("signal","buy","pair","BTC/USD")));
        TaskResult r = worker.execute(t);
        assertEquals(true, r.getOutputData().get("confirmed"));
        assertEquals(true, r.getOutputData().get("portfolioUpdated"));
    }
}
