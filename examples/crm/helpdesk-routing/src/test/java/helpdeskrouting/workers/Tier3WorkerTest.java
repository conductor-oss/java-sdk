package helpdeskrouting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class Tier3WorkerTest {
    private final Tier3Worker worker = new Tier3Worker();
    @Test void taskDefName() { assertEquals("hdr_tier3", worker.getTaskDefName()); }
    @Test void handlesIssue() {
        TaskResult r = worker.execute(taskWith(Map.of("issue", "outage", "customerId", "C1")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("T3 Senior SRE - Nate", r.getOutputData().get("handler"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
