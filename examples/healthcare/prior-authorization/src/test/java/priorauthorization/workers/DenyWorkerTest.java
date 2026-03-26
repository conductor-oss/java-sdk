package priorauthorization.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DenyWorkerTest {
    private final DenyWorker w = new DenyWorker();
    @Test void taskDefName() { assertEquals("pa_deny", w.getTaskDefName()); }
    @Test void denies() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("authId", "A1", "denyReason", "cosmetic")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("denied"));
    }
}
