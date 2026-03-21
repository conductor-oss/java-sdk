package campaignautomation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TargetWorkerTest {
    private final TargetWorker worker = new TargetWorker();
    @Test void taskDefName() { assertEquals("cpa_target", worker.getTaskDefName()); }
    @Test void buildsAudience() {
        TaskResult r = worker.execute(taskWith(Map.of("campaignId", "CMP-1", "budget", 5000)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(125000, r.getOutputData().get("audienceSize"));
        assertNotNull(r.getOutputData().get("audience"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
