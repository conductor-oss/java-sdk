package plagiarismdetection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CompareWorkerTest {
    private final CompareWorker worker = new CompareWorker();
    @Test void taskDefName() { assertEquals("plg_compare", worker.getTaskDefName()); }
    @Test void comparesAndReturnsClean() {
        Task task = taskWith(Map.of("scanResults", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("clean", result.getOutputData().get("verdict"));
        assertEquals(8, result.getOutputData().get("similarityScore"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
