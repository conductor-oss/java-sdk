package dripcampaign.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SendSeriesWorkerTest {
    private final SendSeriesWorker worker = new SendSeriesWorker();
    @Test void taskDefName() { assertEquals("drp_send_series", worker.getTaskDefName()); }
    @Test void sendsSeries() {
        TaskResult r = worker.execute(taskWith(Map.of("email", "a@b.com", "enrollmentId", "ENR-1")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(5, r.getOutputData().get("emailsSent"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
