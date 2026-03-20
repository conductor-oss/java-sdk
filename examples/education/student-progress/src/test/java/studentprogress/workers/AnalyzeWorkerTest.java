package studentprogress.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AnalyzeWorkerTest {
    private final AnalyzeWorker worker = new AnalyzeWorker();
    @Test void taskDefName() { assertEquals("spr_analyze", worker.getTaskDefName()); }
    @Test void analyzesGrades() {
        Task task = taskWith(Map.of("grades", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3.52, result.getOutputData().get("gpa"));
        assertEquals("Dean's List", result.getOutputData().get("standing"));
    }
    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
