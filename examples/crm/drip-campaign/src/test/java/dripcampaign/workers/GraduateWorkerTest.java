package dripcampaign.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GraduateWorkerTest {
    private final GraduateWorker worker = new GraduateWorker();
    @Test void taskDefName() { assertEquals("drp_graduate", worker.getTaskDefName()); }
    @Test void graduatesHighEngagement() {
        TaskResult r = worker.execute(taskWith(Map.of("contactId", "C1", "engagement", 78)));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("graduated"));
        assertEquals("sales_handoff", r.getOutputData().get("nextAction"));
    }
    @Test void recyclesLowEngagement() {
        TaskResult r = worker.execute(taskWith(Map.of("contactId", "C1", "engagement", 30)));
        assertEquals(false, r.getOutputData().get("graduated"));
        assertEquals("recycle_nurture", r.getOutputData().get("nextAction"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
