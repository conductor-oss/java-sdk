package plagiarismdetection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class HandleFlaggedWorkerTest {
    private final HandleFlaggedWorker worker = new HandleFlaggedWorker();
    @Test void taskDefName() { assertEquals("plg_handle_flagged", worker.getTaskDefName()); }
    @Test void handlesFlaggedWork() {
        Task task = taskWith(Map.of("studentId", "STU-001", "similarityScore", 45));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("escalated", result.getOutputData().get("action"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
