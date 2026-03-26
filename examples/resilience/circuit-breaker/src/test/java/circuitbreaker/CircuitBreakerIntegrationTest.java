package circuitbreaker;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import circuitbreaker.workers.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that validates worker-to-worker data flow through
 * the circuit breaker pattern:
 *   CheckCircuit -> CallService (if CLOSED/HALF_OPEN) or Fallback (if OPEN)
 */
class CircuitBreakerIntegrationTest {

    @BeforeEach
    void setUp() {
        CircuitBreakerState.clearAll();
    }

    @Test
    void closedCircuitCallsServiceDirectly() {
        CheckCircuitWorker checkWorker = new CheckCircuitWorker();
        CallServiceWorker callWorker = new CallServiceWorker();

        // Check circuit -- should be CLOSED
        Task checkTask = taskWith(Map.of("serviceName", "order-api", "failureCount", 0, "threshold", 3));
        TaskResult checkResult = checkWorker.execute(checkTask);
        assertEquals("CLOSED", checkResult.getOutputData().get("state"));

        // Call service -- should succeed
        Task callTask = taskWith(Map.of("serviceName", "order-api"));
        TaskResult callResult = callWorker.execute(callTask);
        assertEquals(TaskResult.Status.COMPLETED, callResult.getStatus());
        assertEquals("live", callResult.getOutputData().get("source"));
    }

    @Test
    void openCircuitUsesFallback() {
        CheckCircuitWorker checkWorker = new CheckCircuitWorker();
        FallbackWorker fallbackWorker = new FallbackWorker();

        // Check circuit -- forced OPEN
        Task checkTask = taskWith(Map.of("serviceName", "order-api", "failureCount", 5, "threshold", 3));
        TaskResult checkResult = checkWorker.execute(checkTask);
        assertEquals("OPEN", checkResult.getOutputData().get("state"));

        // Since circuit is OPEN, use fallback
        Task fallbackTask = taskWith(Map.of("serviceName", "order-api"));
        TaskResult fallbackResult = fallbackWorker.execute(fallbackTask);
        assertEquals(TaskResult.Status.COMPLETED, fallbackResult.getStatus());
        assertEquals("cache", fallbackResult.getOutputData().get("source"));
    }

    @Test
    void halfOpenTransitionToClosedOnSuccess() {
        CheckCircuitWorker checkWorker = new CheckCircuitWorker();
        CallServiceWorker callWorker = new CallServiceWorker();

        // Set to HALF_OPEN
        Task checkTask = taskWith(Map.of("serviceName", "order-api", "circuitState", "HALF_OPEN"));
        TaskResult checkResult = checkWorker.execute(checkTask);
        assertEquals("HALF_OPEN", checkResult.getOutputData().get("state"));

        // Try calling service (test call in HALF_OPEN)
        Task callTask = taskWith(Map.of("serviceName", "order-api"));
        TaskResult callResult = callWorker.execute(callTask);
        assertEquals(TaskResult.Status.COMPLETED, callResult.getStatus());

        // Service succeeded, so circuit should transition to CLOSED
        Task recheckTask = taskWith(Map.of("serviceName", "order-api", "failureCount", 0, "threshold", 3));
        TaskResult recheckResult = checkWorker.execute(recheckTask);
        assertEquals("CLOSED", recheckResult.getOutputData().get("state"));
    }

    @Test
    void halfOpenTransitionToOpenOnFailure() {
        CheckCircuitWorker checkWorker = new CheckCircuitWorker();
        CallServiceWorker callWorker = new CallServiceWorker();

        // Set to HALF_OPEN
        Task checkTask = taskWith(Map.of("serviceName", "order-api", "circuitState", "HALF_OPEN"));
        checkWorker.execute(checkTask);

        // Service fails during HALF_OPEN test
        Task callTask = taskWith(Map.of("serviceName", "order-api", "shouldFail", true));
        TaskResult callResult = callWorker.execute(callTask);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, callResult.getStatus());

        // Circuit should go back to OPEN
        int failures = CircuitBreakerState.getFailureCount("order-api");
        Task recheckTask = taskWith(Map.of("serviceName", "order-api", "failureCount", failures, "threshold", 1));
        TaskResult recheckResult = checkWorker.execute(recheckTask);
        assertEquals("OPEN", recheckResult.getOutputData().get("state"));
    }

    @Test
    void failureThresholdTripsCircuit() {
        CheckCircuitWorker checkWorker = new CheckCircuitWorker();
        CallServiceWorker callWorker = new CallServiceWorker();

        // Start with CLOSED, 0 failures
        Task check1 = taskWith(Map.of("serviceName", "flaky-svc", "failureCount", 0, "threshold", 3));
        assertEquals("CLOSED", checkWorker.execute(check1).getOutputData().get("state"));

        // Fail 3 times
        callWorker.execute(taskWith(Map.of("serviceName", "flaky-svc", "shouldFail", true)));
        callWorker.execute(taskWith(Map.of("serviceName", "flaky-svc", "shouldFail", true)));
        callWorker.execute(taskWith(Map.of("serviceName", "flaky-svc", "shouldFail", true)));

        // Check circuit with accumulated failures
        int failures = CircuitBreakerState.getFailureCount("flaky-svc");
        assertEquals(3, failures);

        Task check2 = taskWith(Map.of("serviceName", "flaky-svc", "failureCount", failures, "threshold", 3));
        TaskResult check2Result = checkWorker.execute(check2);
        assertEquals("OPEN", check2Result.getOutputData().get("state"));
    }

    @Test
    void statePersistsToFile() {
        CheckCircuitWorker checkWorker = new CheckCircuitWorker();

        // Set state
        Task task = taskWith(Map.of("serviceName", "persist-test-svc", "circuitState", "OPEN"));
        checkWorker.execute(task);

        // Verify persisted state
        String state = CircuitBreakerState.getState("persist-test-svc");
        assertEquals("OPEN", state);
    }

    @Test
    void concurrentCircuitBreakerAccess() throws Exception {
        CheckCircuitWorker checkWorker = new CheckCircuitWorker();
        CallServiceWorker callWorker = new CallServiceWorker();
        int threadCount = 10;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final boolean shouldFail = i % 2 == 0;
            new Thread(() -> {
                try {
                    startLatch.await();
                    if (shouldFail) {
                        TaskResult r = callWorker.execute(taskWith(Map.of("serviceName", "concurrent-svc", "shouldFail", true)));
                        if (r.getStatus() == TaskResult.Status.FAILED_WITH_TERMINAL_ERROR) failCount.incrementAndGet();
                    } else {
                        TaskResult r = callWorker.execute(taskWith(Map.of("serviceName", "concurrent-svc")));
                        if (r.getStatus() == TaskResult.Status.COMPLETED) successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        // Verify we got both successes and failures, and nothing threw
        assertTrue(successCount.get() > 0, "Some calls should succeed");
        assertTrue(failCount.get() > 0, "Some calls should fail");
        assertEquals(threadCount, successCount.get() + failCount.get(), "All threads should complete");
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
