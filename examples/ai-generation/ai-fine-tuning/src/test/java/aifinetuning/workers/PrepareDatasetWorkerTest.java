package aifinetuning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PrepareDatasetWorkerTest {

    @Test
    void testPrepareDatasetWorker() {
        PrepareDatasetWorker worker = new PrepareDatasetWorker();
        assertEquals("aft_prepare_dataset", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("datasetId", "DS-TEST", "taskType", "test"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(10000, result.getOutputData().get("datasetSize"));
        assertEquals(8000, result.getOutputData().get("trainSplit"));
    }
}
