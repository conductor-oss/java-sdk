package campaignautomation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ExecuteWorkerTest {
    private final ExecuteWorker worker = new ExecuteWorker();
    @Test void taskDefName() { assertEquals("cpa_execute", worker.getTaskDefName()); }
    @Test void executesCampaign() {
        TaskResult r = worker.execute(taskWith(Map.of("campaignId", "CMP-1", "audience", Map.of())));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("executionId"));
        assertEquals(340000, r.getOutputData().get("impressions"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
