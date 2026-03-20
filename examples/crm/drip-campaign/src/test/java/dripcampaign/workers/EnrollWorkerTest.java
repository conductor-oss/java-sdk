package dripcampaign.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EnrollWorkerTest {
    private final EnrollWorker worker = new EnrollWorker();
    @Test void taskDefName() { assertEquals("drp_enroll", worker.getTaskDefName()); }
    @Test void enrollsContact() {
        TaskResult r = worker.execute(taskWith(Map.of("contactId", "C1", "campaignId", "DRP-1")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("enrollmentId"));
        assertNotNull(r.getOutputData().get("enrolledAt"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
