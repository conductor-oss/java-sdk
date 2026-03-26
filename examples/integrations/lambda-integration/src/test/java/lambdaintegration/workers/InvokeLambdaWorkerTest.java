package lambdaintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InvokeLambdaWorkerTest {

    private final InvokeLambdaWorker worker = new InvokeLambdaWorker();

    @Test
    void taskDefName() {
        assertEquals("lam_invoke", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("functionName", "process-orders", "qualifier", "$LATEST", "payload", Map.of("body", "test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(200, result.getOutputData().get("statusCode"));
        assertNotNull(result.getOutputData().get("responsePayload"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
