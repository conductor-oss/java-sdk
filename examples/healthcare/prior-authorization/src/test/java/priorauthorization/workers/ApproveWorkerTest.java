package priorauthorization.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ApproveWorkerTest {
    private final ApproveWorker w = new ApproveWorker();
    @Test void taskDefName() { assertEquals("pa_approve", w.getTaskDefName()); }
    @Test void approves() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("authId", "A1", "validDays", 90)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("approved"));
    }
}
