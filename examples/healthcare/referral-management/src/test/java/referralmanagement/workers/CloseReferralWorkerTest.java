package referralmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CloseReferralWorkerTest {
    private final CloseReferralWorker w = new CloseReferralWorker();
    @Test void taskDefName() { assertEquals("ref_close", w.getTaskDefName()); }
    @Test void closes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("referralId", "R1", "outcome", "completed")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("completed", r.getOutputData().get("closedStatus"));
    }
}
