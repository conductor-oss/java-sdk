package knowledgebasesync.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ExtractWorkerTest {
    private final ExtractWorker worker = new ExtractWorker();
    @Test void taskDefName() { assertEquals("kbs_extract", worker.getTaskDefName()); }
    @Test void extractsArticles() {
        TaskResult r = worker.execute(taskWith(Map.of("pages", List.of("a.html"))));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(89, r.getOutputData().get("articleCount"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
