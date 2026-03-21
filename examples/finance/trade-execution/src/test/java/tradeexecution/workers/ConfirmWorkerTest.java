package tradeexecution.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmWorkerTest {

    private final ConfirmWorker worker = new ConfirmWorker();

    @Test
    void taskDefName() {
        assertEquals("trd_confirm", worker.getTaskDefName());
    }

    @Test
    void confirmsTradeSuccessfully() {
        Task task = taskWith(Map.of("orderId", "ORD-1", "fillPrice", 175.42));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("confirmed"));
        assertEquals("CONF-TRD-88201", result.getOutputData().get("confirmationId"));
        assertEquals("T+1", result.getOutputData().get("settlesOn"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
