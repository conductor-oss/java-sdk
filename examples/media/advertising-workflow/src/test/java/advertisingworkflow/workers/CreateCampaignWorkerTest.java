package advertisingworkflow.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class CreateCampaignWorkerTest {
    private final CreateCampaignWorker worker = new CreateCampaignWorker();
    @Test void taskDefName() { assertEquals("adv_create_campaign", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("CRE-530-001", r.getOutputData().get("creativeId"));
        assertNotNull(r.getOutputData().get("adFormats"));
        assertEquals("2026-03-08T08:00:00Z", r.getOutputData().get("createdAt"));
    }
}
