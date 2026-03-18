package certificaterotation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateWorkerTest {

    private final GenerateWorker worker = new GenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_generate", worker.getTaskDefName());
    }

    @Test
    void generatesRealCertificate() {
        Task task = taskWith(Map.of("generateData", Map.of("domain", "api.example.com")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String pem = (String) result.getOutputData().get("certPem");
        assertNotNull(pem);
        assertTrue(pem.startsWith("-----BEGIN CERTIFICATE-----"));
        assertTrue(pem.endsWith("-----END CERTIFICATE-----"));

        assertNotNull(result.getOutputData().get("serialNumber"));
        assertEquals("SHA256withRSA", result.getOutputData().get("algorithm"));
        assertEquals(2048, result.getOutputData().get("keySize"));
        assertEquals(365, result.getOutputData().get("validDays"));
        assertEquals("api.example.com", result.getOutputData().get("domain"));
    }

    @Test
    void defaultsToLocalhostWhenNoDomain() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("localhost", result.getOutputData().get("domain"));
        assertNotNull(result.getOutputData().get("certPem"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
