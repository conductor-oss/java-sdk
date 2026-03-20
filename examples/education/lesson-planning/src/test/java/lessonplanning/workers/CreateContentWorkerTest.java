package lessonplanning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CreateContentWorkerTest {
    private final CreateContentWorker worker = new CreateContentWorker();
    @Test void taskDefName() { assertEquals("lpl_create_content", worker.getTaskDefName()); }
    @Test void createsContent() {
        Task task = taskWith(Map.of("objectives", List.of("obj1"), "lessonTitle", "BST"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5, result.getOutputData().get("sectionCount"));
        assertNotNull(result.getOutputData().get("lessonPlan"));
    }
    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
