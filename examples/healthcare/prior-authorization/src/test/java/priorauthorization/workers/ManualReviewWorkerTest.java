package priorauthorization.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ManualReviewWorkerTest {
    private final ManualReviewWorker w = new ManualReviewWorker();
    @Test void taskDefName() { assertEquals("pa_manual_review", w.getTaskDefName()); }
    @Test void reviews() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("authId", "A1", "reviewNotes", "needs docs")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("pendingReview"));
    }
}
