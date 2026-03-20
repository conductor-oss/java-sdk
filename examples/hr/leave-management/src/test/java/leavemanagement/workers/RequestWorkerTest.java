package leavemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RequestWorkerTest {
    private final RequestWorker worker = new RequestWorker();

    @Test void taskDefName() { assertEquals("lvm_request", worker.getTaskDefName()); }

    @Test void submitsRequest() {
        Task task = taskWith(Map.of("employeeId", "EMP-400", "leaveType", "vacation", "days", 5));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("LV-601", result.getOutputData().get("requestId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
