package educationenrollment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplyWorkerTest {

    private final ApplyWorker worker = new ApplyWorker();

    @Test
    void taskDefName() {
        assertEquals("edu_apply", worker.getTaskDefName());
    }

    @Test
    void createsApplication() {
        Task task = taskWith(Map.of("studentName", "Jane Smith", "program", "Computer Science", "gpa", 3.8));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("applicationId"));
    }

    @Test
    void applicationIdIsNonEmpty() {
        Task task = taskWith(Map.of("studentName", "John Doe", "program", "Math", "gpa", 3.5));
        TaskResult result = worker.execute(task);

        String appId = (String) result.getOutputData().get("applicationId");
        assertFalse(appId.isEmpty());
    }

    @Test
    void handlesMinimalInput() {
        Task task = taskWith(Map.of("studentName", "A", "program", "B", "gpa", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
