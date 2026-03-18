package escalationchain.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EscSubmitWorkerTest {
    @Test void taskDefName() { assertEquals("esc_submit", new EscSubmitWorker().getTaskDefName()); }

    @Test void submitsSuccessfully() {
        EscSubmitWorker w = new EscSubmitWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("requestId", "REQ-001", "amount", 5000)));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("submitted"));
        assertEquals("Analyst", r.getOutputData().get("firstLevel"));
    }

    @Test void highAmountEscalatesToVP() {
        EscSubmitWorker w = new EscSubmitWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("requestId", "REQ-002", "amount", 200000)));
        TaskResult r = w.execute(t);
        assertEquals("VP", r.getOutputData().get("firstLevel"));
    }
}
