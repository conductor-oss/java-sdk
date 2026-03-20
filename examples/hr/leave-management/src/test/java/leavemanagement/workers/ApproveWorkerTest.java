package leavemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ApproveWorkerTest {
    private final ApproveWorker worker = new ApproveWorker();

    @Test void taskDefName() { assertEquals("lvm_approve", worker.getTaskDefName()); }

    @Test void approvesRequest() {
        Task task = taskWith(Map.of("requestId", "LV-601", "sufficient", true));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("approved"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
