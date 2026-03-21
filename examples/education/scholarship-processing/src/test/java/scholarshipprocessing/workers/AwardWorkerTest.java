package scholarshipprocessing.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AwardWorkerTest {
    @Test void taskDefName() { assertEquals("scp_award", new AwardWorker().getTaskDefName()); }
    @Test void awardsRank1() {
        Task task = taskWith(Map.of("applicationId", "SAPP-001", "rank", 1, "scholarshipId", "MERIT"));
        TaskResult result = new AwardWorker().execute(task);
        assertEquals(true, result.getOutputData().get("awarded"));
        assertEquals(10000, result.getOutputData().get("amount"));
    }
    @Test void doesNotAwardRank3() {
        Task task = taskWith(Map.of("applicationId", "SAPP-002", "rank", 3, "scholarshipId", "MERIT"));
        TaskResult result = new AwardWorker().execute(task);
        assertEquals(false, result.getOutputData().get("awarded"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
