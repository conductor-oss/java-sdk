package contentrecommendation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AnalyzeHistoryWorkerTest {
    private final AnalyzeHistoryWorker worker = new AnalyzeHistoryWorker();
    @Test void taskDefName() { assertEquals("crm_analyze_history", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("viewedItems"));
        assertNotNull(r.getOutputData().get("likedItems"));
        assertNotNull(r.getOutputData().get("topCategories"));
    }
}
