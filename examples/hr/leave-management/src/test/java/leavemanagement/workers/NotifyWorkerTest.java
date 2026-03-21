package leavemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NotifyWorkerTest {
    private final NotifyWorker worker = new NotifyWorker();

    @Test void taskDefName() { assertEquals("lvm_notify", worker.getTaskDefName()); }

    @Test void notifiesEmployee() {
        Task task = taskWith(Map.of("employeeId", "EMP-400", "approved", true));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("notified"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
