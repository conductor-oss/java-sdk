package retrospective.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CategorizeWorkerTest {
    private final CategorizeWorker worker = new CategorizeWorker();

    @Test void taskDefName() { assertEquals("rsp_categorize", worker.getTaskDefName()); }

    @Test void categorizesFeedback() {
        Task task = taskWith(Map.of("sprintId", "SPR-42", "feedback", "test feedback"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("categories"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
