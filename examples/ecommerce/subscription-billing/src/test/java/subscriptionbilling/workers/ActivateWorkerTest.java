package subscriptionbilling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ActivateWorkerTest {
    private final ActivateWorker worker = new ActivateWorker();

    @Test void taskDefName() { assertEquals("sub_activate", worker.getTaskDefName()); }

    @Test void activatesSubscription() {
        Task task = taskWith(Map.of("subscriptionId", "sub-1", "chargeId", "chg-1", "nextPeriodStart", "2024-02-01"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("active"));
        assertEquals("2024-02-01", r.getOutputData().get("nextPeriodStart"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
