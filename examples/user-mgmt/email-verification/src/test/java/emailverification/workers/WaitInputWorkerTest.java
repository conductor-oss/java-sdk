package emailverification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WaitInputWorkerTest {

    private final WaitInputWorker worker = new WaitInputWorker();

    @Test
    void taskDefName() {
        assertEquals("emv_wait_input", worker.getTaskDefName());
    }

    @Test
    void returnsSubmittedCode() {
        Task task = taskWith(Map.of("expectedCode", "482917", "userId", "USR-123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("482917", result.getOutputData().get("submittedCode"));
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("expectedCode", "999999", "userId", "USR-456"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
