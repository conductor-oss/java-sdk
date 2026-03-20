package knowledgebasesync.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CrawlWorkerTest {
    private final CrawlWorker worker = new CrawlWorker();
    @Test void taskDefName() { assertEquals("kbs_crawl", worker.getTaskDefName()); }
    @Test void crawlsPages() {
        TaskResult r = worker.execute(taskWith(Map.of("sourceUrl", "https://docs.example.com")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(156, r.getOutputData().get("pageCount"));
        assertNotNull(r.getOutputData().get("pages"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
