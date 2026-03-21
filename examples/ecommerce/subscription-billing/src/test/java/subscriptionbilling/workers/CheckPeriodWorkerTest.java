package subscriptionbilling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CheckPeriodWorkerTest {
    private final CheckPeriodWorker worker = new CheckPeriodWorker();

    @Test void taskDefName() { assertEquals("sub_check_period", worker.getTaskDefName()); }

    @Test void returnsPeriodDates() {
        Task task = taskWith(Map.of("subscriptionId", "sub-1", "billingCycle", "monthly"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("periodStart"));
        assertNotNull(r.getOutputData().get("periodEnd"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
