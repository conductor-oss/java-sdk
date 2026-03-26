package leavemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CheckBalanceWorkerTest {
    private final CheckBalanceWorker worker = new CheckBalanceWorker();

    @Test void taskDefName() { assertEquals("lvm_check_balance", worker.getTaskDefName()); }

    @Test void checksBalance() {
        Task task = taskWith(Map.of("employeeId", "EMP-400", "leaveType", "vacation", "requested", 5));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(15, result.getOutputData().get("balance"));
        assertEquals(true, result.getOutputData().get("sufficient"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
