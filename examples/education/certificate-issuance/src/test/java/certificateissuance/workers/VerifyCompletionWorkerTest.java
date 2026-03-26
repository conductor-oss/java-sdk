package certificateissuance.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VerifyCompletionWorkerTest {
    private final VerifyCompletionWorker worker = new VerifyCompletionWorker();

    @Test void taskDefName() { assertEquals("cer_verify_completion", worker.getTaskDefName()); }

    @Test void verifiesCompletion() {
        Task task = taskWith(Map.of("studentId", "STU-001", "courseId", "CS-301"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("completed"));
        assertNotNull(result.getOutputData().get("completionDate"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
