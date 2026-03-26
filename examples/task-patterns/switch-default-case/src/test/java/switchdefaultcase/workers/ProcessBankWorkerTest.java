package switchdefaultcase.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessBankWorkerTest {

    private final ProcessBankWorker worker = new ProcessBankWorker();

    @Test
    void taskDefName() {
        assertEquals("dc_process_bank", worker.getTaskDefName());
    }

    @Test
    void returnsPlaidHandler() {
        Task task = taskWith(Map.of("paymentMethod", "bank_transfer"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("plaid", result.getOutputData().get("handler"));
    }

    @Test
    void outputIsAlwaysDeterministic() {
        Task task1 = taskWith(Map.of("paymentMethod", "bank_transfer"));
        Task task2 = taskWith(Map.of("paymentMethod", "bank_transfer"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("handler"), result2.getOutputData().get("handler"));
    }

    @Test
    void statusIsAlwaysCompleted() {
        Task task = taskWith(Map.of("paymentMethod", "bank_transfer"));
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
