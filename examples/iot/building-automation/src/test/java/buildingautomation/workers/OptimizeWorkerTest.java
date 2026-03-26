package buildingautomation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class OptimizeWorkerTest {
    private final OptimizeWorker worker = new OptimizeWorker();
    @Test void taskDefName() { assertEquals("bld_optimize", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("12.3%", r.getOutputData().get("energySaved"));
        assertEquals("$45/day", r.getOutputData().get("costSavings"));
    }
}
