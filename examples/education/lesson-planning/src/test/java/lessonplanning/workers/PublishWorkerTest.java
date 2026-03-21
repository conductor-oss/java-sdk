package lessonplanning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PublishWorkerTest {
    private final PublishWorker worker = new PublishWorker();
    @Test void taskDefName() { assertEquals("lpl_publish", worker.getTaskDefName()); }
    @Test void publishesLesson() {
        Task task = taskWith(Map.of("lessonPlan", Map.of(), "courseId", "CS-201", "week", 6));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
    }
    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
