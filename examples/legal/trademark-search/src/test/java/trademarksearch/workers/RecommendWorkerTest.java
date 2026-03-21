package trademarksearch.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RecommendWorkerTest {
    @Test void taskDefName() { assertEquals("tmk_recommend", new RecommendWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("trademarkName", "Test", "goodsAndServices", "Software", "searchResults", "list", "conflicts", "list", "assessment", "low")));
        TaskResult r = new RecommendWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("recommendation"));
    }
}
