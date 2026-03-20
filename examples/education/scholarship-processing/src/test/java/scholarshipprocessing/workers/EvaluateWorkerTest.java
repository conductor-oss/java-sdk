package scholarshipprocessing.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EvaluateWorkerTest {
    @Test void taskDefName() { assertEquals("scp_evaluate", new EvaluateWorker().getTaskDefName()); }
    @Test void evaluatesHighGpaHighNeed() {
        Task task = taskWith(Map.of("applicationId", "SAPP-001", "gpa", 3.9, "financialNeed", "high"));
        TaskResult result = new EvaluateWorker().execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        int score = (int) result.getOutputData().get("score");
        assertTrue(score >= 90);
        assertEquals(true, result.getOutputData().get("eligible"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
