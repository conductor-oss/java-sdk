package switchdefaultcase.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogWorkerTest {

    private final LogWorker worker = new LogWorker();

    @Test
    void taskDefName() {
        assertEquals("dc_log", worker.getTaskDefName());
    }

    @Test
    void returnsLoggedTrue() {
        Task task = taskWith(Map.of("paymentMethod", "credit_card"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test
    void outputIsAlwaysDeterministic() {
        Task task1 = taskWith(Map.of("paymentMethod", "credit_card"));
        Task task2 = taskWith(Map.of("paymentMethod", "paypal"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("logged"), result2.getOutputData().get("logged"));
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
