package contentsyndication.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class SelectContentWorkerTest {
    private final SelectContentWorker worker = new SelectContentWorker();
    @Test void taskDefName() { assertEquals("syn_select_content", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("Workflow orchestration enables teams to build reliable distributed systems...", r.getOutputData().get("contentBody"));
        assertNotNull(r.getOutputData().get("metadata"));
        assertEquals("technology", r.getOutputData().get("category"));
    }
}
