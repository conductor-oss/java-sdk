package projectkickoff.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CreatePlanWorkerTest {
    private final CreatePlanWorker w = new CreatePlanWorker();
    @Test void taskDefName() { assertEquals("pkf_create_plan", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("projectName","Alpha","sponsor","VP","budget","150000","scope","{}","team","{}","plan","{}")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
