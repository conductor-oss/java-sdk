package emailverification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyCodeWorkerTest {

    private final VerifyCodeWorker worker = new VerifyCodeWorker();

    @Test
    void taskDefName() {
        assertEquals("emv_verify", worker.getTaskDefName());
    }

    @Test
    void matchingCodesReturnTrue() {
        Task task = taskWith(Map.of("submittedCode", "482917", "expectedCode", "482917"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("codeMatch"));
    }

    @Test
    void mismatchedCodesReturnFalse() {
        Task task = taskWith(Map.of("submittedCode", "111111", "expectedCode", "482917"));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("codeMatch"));
    }

    @Test
    void nullSubmittedCodeReturnsFalse() {
        Map<String, Object> input = new HashMap<>();
        input.put("submittedCode", null);
        input.put("expectedCode", "482917");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("codeMatch"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
