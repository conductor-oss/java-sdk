package batchscheduling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PrioritizeJobsWorkerTest {
    @Test void taskDefName() { assertEquals("bs_prioritize_jobs", new PrioritizeJobsWorker().getTaskDefName()); }
    @Test void prioritizesJobs() {
        PrioritizeJobsWorker w = new PrioritizeJobsWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("batchId", "BATCH-1")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
