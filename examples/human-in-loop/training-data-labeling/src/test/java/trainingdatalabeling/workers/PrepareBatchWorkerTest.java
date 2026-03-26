package trainingdatalabeling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrepareBatchWorkerTest {

    @Test
    void taskDefName() {
        PrepareBatchWorker worker = new PrepareBatchWorker();
        assertEquals("tdl_prepare_batch", worker.getTaskDefName());
    }

    @Test
    void returnsPreparedTrue() {
        PrepareBatchWorker worker = new PrepareBatchWorker();
        Task task = taskWith(Map.of("batchId", "batch-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("prepared"));
    }

    @Test
    void completesWithEmptyInput() {
        PrepareBatchWorker worker = new PrepareBatchWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("prepared"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
