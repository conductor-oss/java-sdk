package helpdeskrouting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class Tier1WorkerTest {
    private final Tier1Worker worker = new Tier1Worker();
    @Test void taskDefName() { assertEquals("hdr_tier1", worker.getTaskDefName()); }
    @Test void handlesIssue() {
        TaskResult r = worker.execute(taskWith(Map.of("issue", "test", "customerId", "C1")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("T1 Support - Alex", r.getOutputData().get("handler"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
