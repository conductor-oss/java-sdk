package assessmentcreation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DefineCriteriaWorkerTest {
    private final DefineCriteriaWorker worker = new DefineCriteriaWorker();
    @Test void taskDefName() { assertEquals("asc_define_criteria", worker.getTaskDefName()); }
    @Test void definesCriteria() {
        Task task = taskWith(Map.of("courseId", "CS-201", "assessmentType", "midterm_exam", "topics", List.of("sorting")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("criteriaCount"));
    }
    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
