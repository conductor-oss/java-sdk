package scholarshipprocessing.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ApplyWorkerTest {
    @Test void taskDefName() { assertEquals("scp_apply", new ApplyWorker().getTaskDefName()); }
    @Test void appliesForScholarship() {
        Task task = taskWith(Map.of("studentId", "STU-001", "scholarshipId", "MERIT-2024", "gpa", 3.9));
        TaskResult result = new ApplyWorker().execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("applicationId"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
