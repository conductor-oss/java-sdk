package assessmentcreation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ReviewWorkerTest {
    private final ReviewWorker worker = new ReviewWorker();
    @Test void taskDefName() { assertEquals("asc_review", worker.getTaskDefName()); }
    @Test void reviewsQuestions() {
        Task task = taskWith(Map.of("questions", List.of(Map.of("id", 1))));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("approved", result.getOutputData().get("status"));
    }
    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
