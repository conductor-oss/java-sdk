package foureyesapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SubmitWorkerTest {
    @Test void taskDefName() { assertEquals("fep_submit", new SubmitWorker().getTaskDefName()); }

    @Test void submitsRequest() {
        SubmitWorker w = new SubmitWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("requestId", "REQ-001")));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("submitted"));
        assertNotNull(r.getOutputData().get("submittedAt"));
    }
}
