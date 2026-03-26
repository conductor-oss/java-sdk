package assessmentcreation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PublishWorkerTest {
    private final PublishWorker worker = new PublishWorker();
    @Test void taskDefName() { assertEquals("asc_publish", worker.getTaskDefName()); }
    @Test void publishesAssessment() {
        Task task = taskWith(Map.of("assessmentId", "EXAM-001", "courseId", "CS-201"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
    }
    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
