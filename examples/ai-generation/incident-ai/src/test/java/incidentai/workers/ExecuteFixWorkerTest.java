package incidentai.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ExecuteFixWorkerTest {
    private final ExecuteFixWorker worker = new ExecuteFixWorker();
    @Test void taskDefName() { assertEquals("iai_execute_fix", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("fix","switch gateway");
        t.setInputData(input);
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
