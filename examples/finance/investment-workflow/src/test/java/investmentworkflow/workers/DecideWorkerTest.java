package investmentworkflow.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class DecideWorkerTest {
    private final DecideWorker worker = new DecideWorker();
    @Test void taskDefName() { assertEquals("ivt_decide", worker.getTaskDefName()); }
    @Test void calculatesShares() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("maxInvestment", 25000, "recommendation", "buy")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        int shares = ((Number)r.getOutputData().get("shares")).intValue();
        assertEquals(134, shares);
        assertEquals("buy", r.getOutputData().get("action"));
    }
}
