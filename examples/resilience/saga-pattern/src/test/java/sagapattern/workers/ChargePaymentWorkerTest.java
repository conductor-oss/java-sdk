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
    void failedPaymentReturnsTerminalError() {
        Task task = taskWith(Map.of("tripId", "TRIP-200", "shouldFail", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertEquals("failed", result.getOutputData().get("status"));
        assertNull(result.getOutputData().get("transactionId"));
        assertFalse(BookingStore.PAYMENT_TRANSACTIONS.containsKey("TXN-TRIP-200"));
    }

    @Test
    void shouldFailAsStringTrue() {
        Task task = taskWith(Map.of("tripId", "TRIP-300", "shouldFail", "true"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void failsOnMissingTripId() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void failsOnNegativeAmount() {
        Task task = taskWith(Map.of("tripId", "TRIP-100", "amount", -50.0));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("positive"));
    }

    @Test
    void failsOnZeroAmount() {
        Task task = taskWith(Map.of("tripId", "TRIP-100", "amount", 0));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void failsOnNonNumericAmount() {
        Task task = taskWith(Map.of("tripId", "TRIP-100", "amount", "not-a-number"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void acceptsValidPositiveAmount() {
        Task task = taskWith(Map.of("tripId", "TRIP-100", "amount", 250.00));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void recordsActionInLog() {
        Task task = taskWith(Map.of("tripId", "TRIP-100"));
        worker.execute(task);
        assertTrue(BookingStore.getActionLog().contains("CHARGE_PAYMENT:TXN-TRIP-100"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
