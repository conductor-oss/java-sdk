package sagapattern.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChargePaymentWorkerTest {

    private final ChargePaymentWorker worker = new ChargePaymentWorker();

    @BeforeEach
    void setUp() {
        BookingStore.clear();
    }

    @Test
    void taskDefName() {
        assertEquals("saga_charge_payment", worker.getTaskDefName());
    }

    @Test
    void successfulPaymentCreatesTransaction() {
        Task task = taskWith(Map.of("tripId", "TRIP-100", "shouldFail", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("status"));
        assertEquals("TXN-TRIP-100", result.getOutputData().get("transactionId"));
        assertTrue(BookingStore.PAYMENT_TRANSACTIONS.containsKey("TXN-TRIP-100"));
    }

    @Test
    void failedPaymentDoesNotCreateTransaction() {
        Task task = taskWith(Map.of("tripId", "TRIP-200", "shouldFail", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("failed", result.getOutputData().get("status"));
        assertNull(result.getOutputData().get("transactionId"));
        assertFalse(BookingStore.PAYMENT_TRANSACTIONS.containsKey("TXN-TRIP-200"));
    }

    @Test
    void shouldFailAsStringTrue() {
        Task task = taskWith(Map.of("tripId", "TRIP-300", "shouldFail", "true"));
        TaskResult result = worker.execute(task);
        assertEquals("failed", result.getOutputData().get("status"));
    }

    @Test
    void statusIsAlwaysCompleted() {
        Task task1 = taskWith(Map.of("tripId", "TRIP-A", "shouldFail", true));
        Task task2 = taskWith(Map.of("tripId", "TRIP-B", "shouldFail", false));

        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task1).getStatus());
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(task2).getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
