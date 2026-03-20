package educationenrollment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrientWorkerTest {

    private final OrientWorker worker = new OrientWorker();

    @Test
    void taskDefName() {
        assertEquals("edu_orient", worker.getTaskDefName());
    }

    @Test
    void schedulesOrientation() {
        Task task = taskWith(Map.of("studentId", "STU-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("2024-08-20", result.getOutputData().get("orientationDate"));
        assertEquals("Main Campus", result.getOutputData().get("location"));
    }

    @Test
    void returnsValidDate() {
        Task task = taskWith(Map.of("studentId", "STU-002"));
        TaskResult result = worker.execute(task);

        String date = (String) result.getOutputData().get("orientationDate");
        assertNotNull(date);
        assertFalse(date.isEmpty());
    }

    @Test
    void returnsLocation() {
        Task task = taskWith(Map.of("studentId", "STU-003"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("location"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
