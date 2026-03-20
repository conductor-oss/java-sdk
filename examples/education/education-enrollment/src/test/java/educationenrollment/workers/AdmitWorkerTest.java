package educationenrollment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdmitWorkerTest {

    private final AdmitWorker worker = new AdmitWorker();

    @Test
    void taskDefName() {
        assertEquals("edu_admit", worker.getTaskDefName());
    }

    @Test
    void admitsHighScore() {
        Task task = taskWith(Map.of("applicationId", "APP-001", "reviewScore", 95));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("admitted"));
        assertNotNull(result.getOutputData().get("studentId"));
    }

    @Test
    void admitsBorderlineScore() {
        Task task = taskWith(Map.of("applicationId", "APP-002", "reviewScore", 75));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("admitted"));
    }

    @Test
    void waitlistsLowScore() {
        Task task = taskWith(Map.of("applicationId", "APP-003", "reviewScore", 50));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("admitted"));
    }

    @Test
    void handlesZeroScore() {
        Task task = taskWith(Map.of("applicationId", "APP-004", "reviewScore", 0));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("admitted"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
