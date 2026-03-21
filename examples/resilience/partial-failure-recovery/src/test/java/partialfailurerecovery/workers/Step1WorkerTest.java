package partialfailurerecovery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Step1WorkerTest {

    @Test
    void taskDefName() {
        Step1Worker worker = new Step1Worker();
        assertEquals("pfr_step1", worker.getTaskDefName());
    }

    @Test
    void returnsS1PrefixedResult() {
        Step1Worker worker = new Step1Worker();
        Task task = taskWith(Map.of("data", "hello"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("s1-hello", result.getOutputData().get("result"));
    }

    @Test
    void handlesEmptyData() {
        Step1Worker worker = new Step1Worker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("s1-", result.getOutputData().get("result"));
    }

    @Test
    void handlesNumericData() {
        Step1Worker worker = new Step1Worker();
        Task task = taskWith(Map.of("data", 42));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("s1-42", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
