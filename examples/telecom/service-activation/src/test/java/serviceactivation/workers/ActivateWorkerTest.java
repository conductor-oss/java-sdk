package serviceactivation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ActivateWorkerTest {

    @Test
    void testActivateWorker() {
        ActivateWorker worker = new ActivateWorker();
        assertEquals("sac_activate", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("serviceId", "SVC-service-activation-001", "testPassed", "true"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("activated"));
    }
}
