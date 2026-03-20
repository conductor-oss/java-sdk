package leavemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class UpdateWorkerTest {
    private final UpdateWorker worker = new UpdateWorker();

    @Test void taskDefName() { assertEquals("lvm_update", worker.getTaskDefName()); }

    @Test void updatesBalance() {
        Task task = taskWith(Map.of("employeeId", "EMP-400", "days", 5));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(10, result.getOutputData().get("remainingBalance"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
