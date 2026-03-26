package plagiarismdetection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class HandleCleanWorkerTest {
    private final HandleCleanWorker worker = new HandleCleanWorker();
    @Test void taskDefName() { assertEquals("plg_handle_clean", worker.getTaskDefName()); }
    @Test void handlesCleanWork() {
        Task task = taskWith(Map.of("assignmentId", "ESSAY-01"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("approved", result.getOutputData().get("action"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
