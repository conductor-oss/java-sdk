package hubspotintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssignOwnerWorkerTest {

    private final AssignOwnerWorker worker = new AssignOwnerWorker();

    @Test
    void taskDefName() {
        assertEquals("hs_assign_owner", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("contactId", "hs-123", "industry", "SaaS", "companySize", "50-200"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Sarah Johnson", result.getOutputData().get("ownerName"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
