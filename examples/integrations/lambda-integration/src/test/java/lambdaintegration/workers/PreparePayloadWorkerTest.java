package lambdaintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PreparePayloadWorkerTest {

    private final PreparePayloadWorker worker = new PreparePayloadWorker();

    @Test
    void taskDefName() {
        assertEquals("lam_prepare_payload", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("functionName", "process-orders", "inputData", Map.of("action", "process")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("payload"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
