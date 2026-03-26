package serviceversioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ResolveVersionWorkerTest {
    private final ResolveVersionWorker worker = new ResolveVersionWorker();
    @Test void taskDefName() { assertEquals("sv_resolve_version", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("apiVersion", "v2")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("resolvedVersion"));
    }
}
