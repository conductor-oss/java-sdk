package calendarintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SyncChangesWorkerTest {
    private final SyncChangesWorker worker = new SyncChangesWorker();
    @Test void taskDefName() { assertEquals("cal_sync_changes", worker.getTaskDefName()); }
    @Test void syncsChanges() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("additions", 1, "updates", 1, "deletions", 0)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(2, r.getOutputData().get("changesSynced"));
    }
}
