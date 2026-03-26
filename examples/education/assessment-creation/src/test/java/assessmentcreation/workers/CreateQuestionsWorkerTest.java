package assessmentcreation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CreateQuestionsWorkerTest {
    private final CreateQuestionsWorker worker = new CreateQuestionsWorker();
    @Test void taskDefName() { assertEquals("asc_create_questions", worker.getTaskDefName()); }
    @Test void createsQuestions() {
        Task task = taskWith(Map.of("criteria", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4, result.getOutputData().get("questionCount"));
        assertNotNull(result.getOutputData().get("assessmentId"));
    }
    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
