package contentarchival.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class IdentifyContentWorkerTest {
    private final IdentifyContentWorker worker = new IdentifyContentWorker();
    @Test void taskDefName() { assertEquals("car_identify_content", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(2450, r.getOutputData().get("itemCount"));
        assertEquals(15200, r.getOutputData().get("totalSizeMb"));
        assertEquals("2024-01-15", r.getOutputData().get("oldestItem"));
    }
}
