package telecomprovisioning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ActivateWorkerTest {
    @Test
    void testActivateWorker() {
        ActivateWorker worker = new ActivateWorker();
        assertEquals("tpv_activate", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("configId", "CFG-telecom-provisioning-A"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SVC-telecom-provisioning-001", result.getOutputData().get("serviceId"));
    }
}
