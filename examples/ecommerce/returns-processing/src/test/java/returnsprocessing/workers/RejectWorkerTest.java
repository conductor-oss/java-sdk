package returnsprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RejectWorkerTest {

    private final RejectWorker worker = new RejectWorker();

    @Test
    void taskDefName() { assertEquals("ret_reject", worker.getTaskDefName()); }

    @Test
    void rejectsReturn() {
        Task task = taskWith(Map.of("returnId", "RET-1", "orderId", "ORD-1", "reason", "Outside return window"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("rejected"));
        assertEquals("Outside return window", r.getOutputData().get("reason"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
