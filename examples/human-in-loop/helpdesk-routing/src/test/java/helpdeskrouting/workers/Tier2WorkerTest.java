package helpdeskrouting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class Tier2WorkerTest {
    private final Tier2Worker worker = new Tier2Worker();
    @Test void taskDefName() { assertEquals("hdr_tier2", worker.getTaskDefName()); }
    @Test void handlesIssue() {
        TaskResult r = worker.execute(taskWith(Map.of("issue", "API error", "customerId", "C1")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("T2 Engineer - Priya", r.getOutputData().get("handler"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
