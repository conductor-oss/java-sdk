package circuitbreaker.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CallServiceWorkerTest {

    @Test
    void taskDefName() {
        CallServiceWorker worker = new CallServiceWorker();
        assertEquals("cb_call_service", worker.getTaskDefName());
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
    void usesDefaultServiceNameWhenNotProvided() {
        CallServiceWorker worker = new CallServiceWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Service default-service responded successfully", result.getOutputData().get("result"));
        assertEquals("live", result.getOutputData().get("source"));
        assertEquals("default-service", result.getOutputData().get("serviceName"));
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
