package riskmanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AssessWorkerTest {
    private final AssessWorker w = new AssessWorker();
    @Test void taskDefName() { assertEquals("rkm_assess", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("projectId","PROJ-42","description","risk","risk","{}","severity","high")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
