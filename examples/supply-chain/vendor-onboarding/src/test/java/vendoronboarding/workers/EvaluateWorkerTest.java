package vendoronboarding.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvaluateWorkerTest {

    private final EvaluateWorker worker = new EvaluateWorker();

    @Test
    void taskDefName() {
        assertEquals("von_evaluate", worker.getTaskDefName());
    }

    @Test
    void evaluatesVendor() {
        Task task = taskWith(Map.of("vendorId", "VND-001", "verificationResult", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(88, result.getOutputData().get("score"));
        assertEquals("gold", result.getOutputData().get("tier"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
