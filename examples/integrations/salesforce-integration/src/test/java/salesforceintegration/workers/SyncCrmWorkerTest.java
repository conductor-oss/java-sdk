package salesforceintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SyncCrmWorkerTest {

    private final SyncCrmWorker worker = new SyncCrmWorker();

    @Test
    void taskDefName() {
        assertEquals("sfc_sync_crm", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("updatedCount", 3, "syncTarget", "marketing-hub"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("synced"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
