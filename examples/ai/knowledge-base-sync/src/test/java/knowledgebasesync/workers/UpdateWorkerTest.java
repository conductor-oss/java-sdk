package knowledgebasesync.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class UpdateWorkerTest {
    private final UpdateWorker worker = new UpdateWorker();
    @Test void taskDefName() { assertEquals("kbs_update", worker.getTaskDefName()); }
    @Test void updatesKb() {
        TaskResult r = worker.execute(taskWith(Map.of("kbId", "KB-1", "articles", List.of())));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(89, r.getOutputData().get("updatedCount"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
