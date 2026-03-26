package lessonplanning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DefineObjectivesWorkerTest {
    private final DefineObjectivesWorker worker = new DefineObjectivesWorker();
    @Test void taskDefName() { assertEquals("lpl_define_objectives", worker.getTaskDefName()); }
    @Test void definesObjectives() {
        Task task = taskWith(Map.of("courseId", "CS-201", "lessonTitle", "BST"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("objectiveCount"));
    }
    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
