package circuitbreaker.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CallServiceWorkerTest {

    @BeforeEach
    void setUp() {
        CircuitBreakerState.clearAll();
    }

    @Test
    void taskDefName() {
        CallServiceWorker worker = new CallServiceWorker();
        assertEquals("cb_call_service", worker.getTaskDefName());
    }

    @Test
    void failsOnMissingServiceName() {
        CallServiceWorker worker = new CallServiceWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void returnsSuccessfulServiceResponse() {
        CallServiceWorker worker = new CallServiceWorker();
        Task task = taskWith(Map.of("serviceName", "payment-api"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Service payment-api responded successfully", result.getOutputData().get("result"));
        assertEquals("live", result.getOutputData().get("source"));
        assertEquals("payment-api", result.getOutputData().get("serviceName"));
    }

    @Test
    void failedServiceReturnsTerminalError() {
        CallServiceWorker worker = new CallServiceWorker();
        Task task = taskWith(Map.of("serviceName", "payment-api", "shouldFail", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertEquals("payment-api", result.getOutputData().get("serviceName"));
    }

    @Test
    void failedServiceIncrementsFailureCount() {
        CallServiceWorker worker = new CallServiceWorker();

        // Fail 3 times
        worker.execute(taskWith(Map.of("serviceName", "payment-api", "shouldFail", true)));
        worker.execute(taskWith(Map.of("serviceName", "payment-api", "shouldFail", true)));
        TaskResult r3 = worker.execute(taskWith(Map.of("serviceName", "payment-api", "shouldFail", true)));

        assertEquals(3, ((Number) r3.getOutputData().get("failureCount")).intValue());
    }

    @Test
    void successResetsFailureCount() {
        CallServiceWorker worker = new CallServiceWorker();

        // Fail twice
        worker.execute(taskWith(Map.of("serviceName", "payment-api", "shouldFail", true)));
        worker.execute(taskWith(Map.of("serviceName", "payment-api", "shouldFail", true)));

        // Succeed
        worker.execute(taskWith(Map.of("serviceName", "payment-api")));

        // Verify failure count is reset
        assertEquals(0, CircuitBreakerState.getFailureCount("payment-api"));
    }

    @Test
    void outputContainsRequiredFields() {
        CallServiceWorker worker = new CallServiceWorker();
        Task task = taskWith(Map.of("serviceName", "user-api"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("source"));
        assertTrue(result.getOutputData().containsKey("serviceName"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
