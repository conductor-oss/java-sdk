package educationenrollment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnrollWorkerTest {

    private final EnrollWorker worker = new EnrollWorker();

    @Test
    void taskDefName() {
        assertEquals("edu_enroll", worker.getTaskDefName());
    }

    @Test
    void enrollsStudent() {
        Task task = taskWith(Map.of("studentId", "STU-001", "program", "Computer Science"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("enrolled"));
        assertNotNull(result.getOutputData().get("semester"));
    }

    @Test
    void returnsSemester() {
        Task task = taskWith(Map.of("studentId", "STU-002", "program", "Math"));
        TaskResult result = worker.execute(task);

        assertEquals("Fall 2024", result.getOutputData().get("semester"));
    }

    @Test
    void handlesAnyProgram() {
        Task task = taskWith(Map.of("studentId", "STU-003", "program", "Biology"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("enrolled"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
