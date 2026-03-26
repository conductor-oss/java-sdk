package knowledgebasesync.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class IndexWorkerTest {
    private final IndexWorker worker = new IndexWorker();
    @Test void taskDefName() { assertEquals("kbs_index", worker.getTaskDefName()); }
    @Test void indexesArticles() {
        TaskResult r = worker.execute(taskWith(Map.of("kbId", "KB-1", "updatedCount", 89)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(89, r.getOutputData().get("indexedCount"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
