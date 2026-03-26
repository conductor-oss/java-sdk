package profileupdate.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SyncProfileWorkerTest {

    private final SyncProfileWorker worker = new SyncProfileWorker();

    @Test
    void taskDefName() {
        assertEquals("pfu_sync", worker.getTaskDefName());
    }

    @Test
    void syncsSuccessfully() {
        Task task = taskWith(Map.of("userId", "USR-123", "updatedFields", List.of("name")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("synced"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsServiceList() {
        Task task = taskWith(Map.of("userId", "USR-123", "updatedFields", List.of("name")));
        TaskResult result = worker.execute(task);

        List<String> services = (List<String>) result.getOutputData().get("services");
        assertEquals(3, services.size());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
