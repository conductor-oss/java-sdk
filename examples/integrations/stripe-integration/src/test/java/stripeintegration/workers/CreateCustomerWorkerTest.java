package stripeintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreateCustomerWorkerTest {

    private final CreateCustomerWorker worker = new CreateCustomerWorker();

    @Test
    void taskDefName() {
        assertEquals("stp_create_customer", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("email", "test@example.com"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("customerId"));
        assertTrue(((String) result.getOutputData().get("customerId")).startsWith("cus_"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
