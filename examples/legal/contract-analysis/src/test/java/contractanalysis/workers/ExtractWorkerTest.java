package contractanalysis.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ExtractWorkerTest {
    @Test void taskDefName() { assertEquals("cna_extract", new ExtractWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, new ExtractWorker().execute(t).getStatus());
    }
}
