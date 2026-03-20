package idempotentworkers.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SendEmailWorkerTest {

    @Test
    void taskDefName() {
        SendEmailWorker worker = new SendEmailWorker();
        assertEquals("idem_send_email", worker.getTaskDefName());
    }

    @Test
    void firstCallSendsEmail() {
        SendEmailWorker worker = new SendEmailWorker();
        Task task = taskWith(Map.of("orderId", "ORD-100", "email", "alice@example.com"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
        assertEquals(false, result.getOutputData().get("duplicate"));
        assertEquals("ORD-100", result.getOutputData().get("orderId"));
        assertEquals("alice@example.com", result.getOutputData().get("email"));
    }

    @Test
    void secondCallReturnsDuplicate() {
        SendEmailWorker worker = new SendEmailWorker();

        Task task1 = taskWith(Map.of("orderId", "ORD-200", "email", "bob@example.com"));
        worker.execute(task1);

        // Second call with the same orderId:email
        Task task2 = taskWith(Map.of("orderId", "ORD-200", "email", "bob@example.com"));
        TaskResult result = worker.execute(task2);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
        assertEquals(true, result.getOutputData().get("duplicate"));
    }

    @Test
    void differentEmailsProcessedIndependently() {
        SendEmailWorker worker = new SendEmailWorker();

        Task task1 = taskWith(Map.of("orderId", "ORD-300", "email", "carol@example.com"));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("orderId", "ORD-300", "email", "dave@example.com"));
        TaskResult result2 = worker.execute(task2);

        assertEquals(false, result1.getOutputData().get("duplicate"));
        assertEquals(false, result2.getOutputData().get("duplicate"));
    }

    @Test
    void differentOrdersSameEmailProcessedIndependently() {
        SendEmailWorker worker = new SendEmailWorker();

        Task task1 = taskWith(Map.of("orderId", "ORD-400", "email", "eve@example.com"));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("orderId", "ORD-401", "email", "eve@example.com"));
        TaskResult result2 = worker.execute(task2);

        assertEquals(false, result1.getOutputData().get("duplicate"));
        assertEquals(false, result2.getOutputData().get("duplicate"));
    }

    @Test
    void thirdCallStillReturnsDuplicate() {
        SendEmailWorker worker = new SendEmailWorker();

        Task task1 = taskWith(Map.of("orderId", "ORD-500", "email", "frank@example.com"));
        worker.execute(task1);

        Task task2 = taskWith(Map.of("orderId", "ORD-500", "email", "frank@example.com"));
        worker.execute(task2);

        // Third call still returns duplicate
        Task task3 = taskWith(Map.of("orderId", "ORD-500", "email", "frank@example.com"));
        TaskResult result = worker.execute(task3);

        assertEquals(true, result.getOutputData().get("duplicate"));
        assertEquals(true, result.getOutputData().get("sent"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        SendEmailWorker worker = new SendEmailWorker();
        Task task = taskWith(Map.of("orderId", "ORD-600", "email", "grace@example.com"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("sent"));
        assertTrue(result.getOutputData().containsKey("duplicate"));
        assertTrue(result.getOutputData().containsKey("orderId"));
        assertTrue(result.getOutputData().containsKey("email"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
