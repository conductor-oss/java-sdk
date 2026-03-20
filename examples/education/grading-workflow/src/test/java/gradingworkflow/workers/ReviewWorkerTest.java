package gradingworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReviewWorkerTest {
    private final ReviewWorker worker = new ReviewWorker();

    @Test void taskDefName() { assertEquals("grd_review", worker.getTaskDefName()); }

    @Test void reviewsGrade() {
        Task task = taskWith(Map.of("submissionId", "SUB-001", "score", 88));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(88, result.getOutputData().get("finalScore"));
        assertEquals(true, result.getOutputData().get("reviewed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
