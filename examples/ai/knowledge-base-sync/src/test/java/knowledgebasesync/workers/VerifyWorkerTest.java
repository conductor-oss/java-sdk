package knowledgebasesync.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VerifyWorkerTest {
    private final VerifyWorker worker = new VerifyWorker();
    @Test void taskDefName() { assertEquals("kbs_verify", worker.getTaskDefName()); }
    @Test void verifiesIndex() {
        TaskResult r = worker.execute(taskWith(Map.of("kbId", "KB-1", "indexedCount", 89)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("searchReady"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
