package studentprogress.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CollectGradesWorkerTest {
    private final CollectGradesWorker worker = new CollectGradesWorker();
    @Test void taskDefName() { assertEquals("spr_collect_grades", worker.getTaskDefName()); }
    @Test void collectsGrades() {
        Task task = taskWith(Map.of("studentId", "STU-001", "semester", "Spring 2024"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4, result.getOutputData().get("courseCount"));
        assertNotNull(result.getOutputData().get("grades"));
    }
    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
