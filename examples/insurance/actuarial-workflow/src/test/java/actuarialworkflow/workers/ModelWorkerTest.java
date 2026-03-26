package actuarialworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ModelWorkerTest {

    @Test
    void testModelWorker() {
        ModelWorker worker = new ModelWorker();
        assertEquals("act_model", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("dataSet", "data", "modelType", "monte-carlo"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("MDL-actuarial-workflow-001", result.getOutputData().get("modelId"));
    }
}
