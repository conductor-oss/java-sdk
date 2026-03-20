package serviceregistry.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RegisterServiceWorkerTest {

    private final RegisterServiceWorker worker = new RegisterServiceWorker();

    @Test
    void taskDefName() {
        assertEquals("sr_register_service", worker.getTaskDefName());
    }

    @Test
    void registersServiceSuccessfully() {
        Task task = taskWith(Map.of("serviceName", "order-service", "serviceUrl", "http://order-svc:8080", "version", "2.1.0"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("registrationId"));
        assertEquals(true, result.getOutputData().get("registered"));
    }

    @Test
    void registrationIdIsDeterministic() {
        Task task1 = taskWith(Map.of("serviceName", "order-service", "serviceUrl", "http://order-svc:8080"));
        Task task2 = taskWith(Map.of("serviceName", "order-service", "serviceUrl", "http://order-svc:8080"));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("registrationId"), r2.getOutputData().get("registrationId"));
    }

    @Test
    void differentServicesGetDifferentIds() {
        Task task1 = taskWith(Map.of("serviceName", "order-service", "serviceUrl", "http://order-svc:8080"));
        Task task2 = taskWith(Map.of("serviceName", "payment-service", "serviceUrl", "http://pay-svc:8080"));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertNotEquals(r1.getOutputData().get("registrationId"), r2.getOutputData().get("registrationId"));
    }

    @Test
    void handlesNullServiceName() {
        Map<String, Object> input = new HashMap<>();
        input.put("serviceName", null);
        input.put("serviceUrl", "http://svc:8080");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("registrationId"));
    }

    @Test
    void handlesNullServiceUrl() {
        Map<String, Object> input = new HashMap<>();
        input.put("serviceName", "test-service");
        input.put("serviceUrl", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("registered"));
    }

    @Test
    void registrationIdStartsWithREG() {
        Task task = taskWith(Map.of("serviceName", "my-service", "serviceUrl", "http://svc:8080"));
        TaskResult result = worker.execute(task);

        String regId = (String) result.getOutputData().get("registrationId");
        assertTrue(regId.startsWith("REG-"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
