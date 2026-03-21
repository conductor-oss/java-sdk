package contentrecommendation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ComputeSimilarityWorkerTest {
    private final ComputeSimilarityWorker worker = new ComputeSimilarityWorker();
    @Test void taskDefName() { assertEquals("crm_compute_similarity", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("similarItems"));
        assertEquals("VID-550", r.getOutputData().get("id"));
        assertNotNull(r.getOutputData().get("score"));
    }
}
