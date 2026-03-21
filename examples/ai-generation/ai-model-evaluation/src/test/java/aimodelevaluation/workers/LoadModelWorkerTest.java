package aimodelevaluation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LoadModelWorkerTest {

    @Test
    void testLoadModelWorker() {
        LoadModelWorker worker = new LoadModelWorker();
        assertEquals("ame_load_model", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("modelId", "MDL-TEST"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("http://model-server/v1/predict", result.getOutputData().get("endpoint"));
    }
}
