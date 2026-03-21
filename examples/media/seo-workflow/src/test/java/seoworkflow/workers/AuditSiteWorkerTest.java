package seoworkflow.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AuditSiteWorkerTest {
    private final AuditSiteWorker worker = new AuditSiteWorker();
    @Test void taskDefName() { assertEquals("seo_audit_site", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(72, r.getOutputData().get("seoScore"));
        assertNotNull(r.getOutputData().get("issues"));
        assertNotNull(r.getOutputData().get("currentRankings"));
    }
}
