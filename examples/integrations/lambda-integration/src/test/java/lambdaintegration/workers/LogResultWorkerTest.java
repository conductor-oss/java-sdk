package lambdaintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogResultWorkerTest {

    private final LogResultWorker worker = new LogResultWorker();

    @Test
    void taskDefName() {
        assertEquals("lam_log_result", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("functionName", "process-orders", "executionResult", "ok", "duration", 250));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
        assertEquals("/aws/lambda/process-orders", result.getOutputData().get("logGroup"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
