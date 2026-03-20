package educationenrollment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReviewWorkerTest {

    private final ReviewWorker worker = new ReviewWorker();

    @Test
    void taskDefName() {
        assertEquals("edu_review", worker.getTaskDefName());
    }

    @Test
    void calculatesScoreFromGpa() {
        Task task = taskWith(Map.of("applicationId", "APP-001", "gpa", 3.8));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(95, result.getOutputData().get("score"));
        assertEquals("admissions-committee", result.getOutputData().get("reviewer"));
    }

    @Test
    void capsScoreAt100() {
        Task task = taskWith(Map.of("applicationId", "APP-002", "gpa", 4.5));
        TaskResult result = worker.execute(task);

        assertEquals(100, (int) result.getOutputData().get("score"));
    }

    @Test
    void handlesZeroGpa() {
        Task task = taskWith(Map.of("applicationId", "APP-003", "gpa", 0));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("score"));
    }

    @Test
    void handlesStringGpa() {
        Task task = taskWith(Map.of("applicationId", "APP-004", "gpa", "3.0"));
        TaskResult result = worker.execute(task);

        assertEquals(75, result.getOutputData().get("score"));
    }

    @Test
    void handlesNullGpa() {
        Map<String, Object> input = new HashMap<>();
        input.put("applicationId", "APP-005");
        input.put("gpa", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("score"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
