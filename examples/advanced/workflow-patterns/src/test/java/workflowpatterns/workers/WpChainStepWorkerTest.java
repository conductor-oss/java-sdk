package workflowpatterns.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WpChainStepWorkerTest {

    private final WpChainStepWorker worker = new WpChainStepWorker();

    @Test
    void taskDefName() {
        assertEquals("wp_chain_step", worker.getTaskDefName());
    }

    @Test
    void processesDataSuccessfully() {
        Task task = taskWith(Map.of("step", "chain", "data", "sample_payload"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("chain_processed", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("step", "chain");
        input.put("data", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("chain_processed", result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingDataKey() {
        Task task = taskWith(Map.of("step", "chain"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("chain_processed", result.getOutputData().get("result"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void resultIsAlwaysChainProcessed() {
        Task task = taskWith(Map.of("data", "any_value"));
        TaskResult result = worker.execute(task);

        assertEquals("chain_processed", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsResultKey() {
        Task task = taskWith(Map.of("data", "test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
    }

    @Test
    void handlesVariousDataValues() {
        Task task = taskWith(Map.of("data", "large_dataset_xyz"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("chain_processed", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
