package leadnurturing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SendWorkerTest {
    private final SendWorker worker = new SendWorker();

    @Test void taskDefName() { assertEquals("nur_send", worker.getTaskDefName()); }

    @Test void sendsEmail() {
        TaskResult r = worker.execute(taskWith(Map.of("leadId", "L1", "content", Map.of())));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("sent"));
        assertNotNull(r.getOutputData().get("deliveryId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
