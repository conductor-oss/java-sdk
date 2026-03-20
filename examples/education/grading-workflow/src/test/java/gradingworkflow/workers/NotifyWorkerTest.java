package gradingworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NotifyWorkerTest {
    private final NotifyWorker worker = new NotifyWorker();

    @Test void taskDefName() { assertEquals("grd_notify", worker.getTaskDefName()); }

    @Test void notifiesStudent() {
        Task task = taskWith(Map.of("studentId", "STU-001", "finalScore", 88));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("notified"));
        assertEquals("email", result.getOutputData().get("method"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
