package campaignautomation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DesignWorkerTest {
    private final DesignWorker worker = new DesignWorker();
    @Test void taskDefName() { assertEquals("cpa_design", worker.getTaskDefName()); }
    @Test void createsCampaign() {
        TaskResult r = worker.execute(taskWith(Map.of("name", "Test", "type", "email")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("campaignId"));
        assertEquals(5, r.getOutputData().get("creativeAssets"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
