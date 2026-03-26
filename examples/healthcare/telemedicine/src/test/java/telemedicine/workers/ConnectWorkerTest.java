package telemedicine.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConnectWorkerTest {
    private final ConnectWorker w = new ConnectWorker();
    @Test void taskDefName() { assertEquals("tlm_connect", w.getTaskDefName()); }
    @Test void connects() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("visitId", "V1", "sessionUrl", "https://example.com")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("CONN-TLM-441", r.getOutputData().get("connectionId"));
    }
}
