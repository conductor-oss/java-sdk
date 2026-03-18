package foureyesapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FinalizeWorkerTest {
    @Test void taskDefName() { assertEquals("fep_finalize", new FinalizeWorker().getTaskDefName()); }

    @Test void bothApproved() {
        FinalizeWorker w = new FinalizeWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("approval1", true, "approval2", true)));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("bothApproved"));
    }

    @Test void oneRejected() {
        FinalizeWorker w = new FinalizeWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("approval1", true, "approval2", false)));
        TaskResult r = w.execute(t);
        assertEquals(false, r.getOutputData().get("bothApproved"));
    }
}
