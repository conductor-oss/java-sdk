package humantask.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectDataWorkerTest {

    @Test
    void taskDefName() {
        CollectDataWorker worker = new CollectDataWorker();
        assertEquals("ht_collect_data", worker.getTaskDefName());
    }

    @Test
    void returnsCollectedTrue() {
        CollectDataWorker worker = new CollectDataWorker();
        Task task = taskWith(Map.of("applicantName", "Jane Doe"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("collected"));
    }

    @Test
    void returnsCollectedTrueWithEmptyInput() {
        CollectDataWorker worker = new CollectDataWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("collected"));
    }

    @Test
    void outputContainsCollectedKey() {
        CollectDataWorker worker = new CollectDataWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("collected"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
