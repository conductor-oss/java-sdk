package usergeneratedcontent.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ModerateWorkerTest {
    private final ModerateWorker worker = new ModerateWorker();
    @Test void taskDefName() { assertEquals("ugc_moderate", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("moderationScore"));
        assertEquals(false, r.getOutputData().get("flagged"));
        assertNotNull(r.getOutputData().get("categories"));
    }
}
