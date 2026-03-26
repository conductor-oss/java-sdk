package programevaluation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class RecommendWorkerTest {
    @Test void testExecute() { RecommendWorker w = new RecommendWorker(); assertEquals("pev_recommend", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of());
        assertNotNull(w.execute(t).getOutputData().get("evaluation")); }
}
