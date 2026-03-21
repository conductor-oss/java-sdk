package scholarshipprocessing.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RankWorkerTest {
    @Test void taskDefName() { assertEquals("scp_rank", new RankWorker().getTaskDefName()); }
    @Test void ranksHighScore() {
        Task task = taskWith(Map.of("score", 95, "applicationId", "SAPP-001"));
        TaskResult result = new RankWorker().execute(task);
        assertEquals(1, result.getOutputData().get("rank"));
    }
    @Test void ranksMidScore() {
        Task task = taskWith(Map.of("score", 85, "applicationId", "SAPP-002"));
        TaskResult result = new RankWorker().execute(task);
        assertEquals(2, result.getOutputData().get("rank"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
