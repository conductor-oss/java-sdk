package switchdefaultcase.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessCryptoWorkerTest {

    private final ProcessCryptoWorker worker = new ProcessCryptoWorker();

    @Test
    void taskDefName() {
        assertEquals("dc_process_crypto", worker.getTaskDefName());
    }

    @Test
    void returnsCoinbaseHandler() {
        Task task = taskWith(Map.of("paymentMethod", "crypto"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("coinbase", result.getOutputData().get("handler"));
    }

    @Test
    void outputIsAlwaysDeterministic() {
        Task task1 = taskWith(Map.of("paymentMethod", "crypto"));
        Task task2 = taskWith(Map.of("paymentMethod", "crypto"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("handler"), result2.getOutputData().get("handler"));
    }

    @Test
    void statusIsAlwaysCompleted() {
        Task task = taskWith(Map.of("paymentMethod", "crypto"));
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
