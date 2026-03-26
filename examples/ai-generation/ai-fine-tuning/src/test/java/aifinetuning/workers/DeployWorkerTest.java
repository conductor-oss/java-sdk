package aifinetuning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DeployWorkerTest {

    @Test
    void testDeployWorker() {
        DeployWorker worker = new DeployWorker();
        assertEquals("aft_deploy", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("modelId", "MDL-TEST", "metrics", "test"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deployed"));
        assertEquals(3, result.getOutputData().get("replicas"));
    }
}
