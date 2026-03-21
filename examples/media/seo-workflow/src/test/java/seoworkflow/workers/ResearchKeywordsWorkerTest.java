package seoworkflow.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ResearchKeywordsWorkerTest {
    private final ResearchKeywordsWorker worker = new ResearchKeywordsWorker();
    @Test void taskDefName() { assertEquals("seo_research_keywords", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("topKeywords"));
        assertEquals("workflow automation", r.getOutputData().get("keyword"));
        assertEquals(12000, r.getOutputData().get("volume"));
    }
}
