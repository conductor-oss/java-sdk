package partialfailurerecovery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Step3WorkerTest {

    @Test
    void taskDefName() {
        Step3Worker worker = new Step3Worker();
        assertEquals("pfr_step3", worker.getTaskDefName());
    }

    @Test
    void returnsS3PrefixedResult() {
        Step3Worker worker = new Step3Worker();
        Task task = taskWith(Map.of("prev", "s2-s1-hello"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("s3-s2-s1-hello", result.getOutputData().get("result"));
    }

    @Test
    void handlesEmptyPrev() {
        Step3Worker worker = new Step3Worker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("s3-", result.getOutputData().get("result"));
    }

    @Test
    void handlesNumericPrev() {
        Step3Worker worker = new Step3Worker();
        Task task = taskWith(Map.of("prev", 123));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("s3-123", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
