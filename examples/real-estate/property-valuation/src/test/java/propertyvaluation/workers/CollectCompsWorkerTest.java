package propertyvaluation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CollectCompsWorkerTest {
    @Test void taskDefName() { assertEquals("pvl_collect_comps", new CollectCompsWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, new CollectCompsWorker().execute(t).getStatus());
    }
}
