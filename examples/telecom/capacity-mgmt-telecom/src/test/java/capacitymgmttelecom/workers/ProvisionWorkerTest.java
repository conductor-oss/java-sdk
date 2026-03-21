package capacitymgmttelecom.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ProvisionWorkerTest {

    @Test
    void testProvisionWorker() {
        ProvisionWorker worker = new ProvisionWorker();
        assertEquals("cmt_provision", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("plan", "expansion"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("150%", result.getOutputData().get("newCapacity"));
        assertEquals(2, result.getOutputData().get("towersAdded"));
    }
}
