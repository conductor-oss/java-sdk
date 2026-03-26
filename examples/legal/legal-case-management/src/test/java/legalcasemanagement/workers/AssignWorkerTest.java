package legalcasemanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AssignWorkerTest {
    @Test void taskDefName() { assertEquals("lcm_assign", new AssignWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, new AssignWorker().execute(t).getStatus());
    }
}
