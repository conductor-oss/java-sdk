package trademarksearch.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AssessWorkerTest {
    @Test void taskDefName() { assertEquals("tmk_assess", new AssessWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("trademarkName", "Test", "goodsAndServices", "Software", "searchResults", "list", "conflicts", "list", "assessment", "low")));
        TaskResult r = new AssessWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("riskLevel"));
    }
}
