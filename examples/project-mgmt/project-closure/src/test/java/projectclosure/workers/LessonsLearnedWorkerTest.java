package projectclosure.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LessonsLearnedWorkerTest {
    private final LessonsLearnedWorker worker = new LessonsLearnedWorker();

    @Test void taskDefName() { assertEquals("pcl_lessons_learned", worker.getTaskDefName()); }

    @Test void documentsLessons() {
        Task task = taskWith(Map.of("projectId", "PRJ-909", "archive", "ARC-909"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("report"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
