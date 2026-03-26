package signals.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SigPrepareWorkerTest {

    private final SigPrepareWorker worker = new SigPrepareWorker();

    @Test
    void taskDefName() {
        assertEquals("sig_prepare", worker.getTaskDefName());
    }

    @Test
    void preparesOrderSuccessfully() {
        Task task = taskWith(Map.of("orderId", "ORD-100"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-100", result.getOutputData().get("orderId"));
        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void returnsReadyTrue() {
        Task task = taskWith(Map.of("orderId", "ORD-200"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void passesOrderIdThrough() {
        Task task = taskWith(Map.of("orderId", "ORD-999"));
        TaskResult result = worker.execute(task);

        assertEquals("ORD-999", result.getOutputData().get("orderId"));
    }

    @Test
    void defaultsOrderIdWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("orderId"));
        assertEquals(true, result.getOutputData().get("ready"));
    }

    @Test
    void defaultsOrderIdWhenBlank() {
        Task task = taskWith(Map.of("orderId", "   "));
        TaskResult result = worker.execute(task);

        assertEquals("unknown", result.getOutputData().get("orderId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
