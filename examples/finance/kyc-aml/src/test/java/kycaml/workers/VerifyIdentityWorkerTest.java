package kycaml.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerifyIdentityWorkerTest {

    private final VerifyIdentityWorker worker = new VerifyIdentityWorker();

    @Test
    void taskDefName() {
        assertEquals("kyc_verify_identity", worker.getTaskDefName());
    }

    @Test
    void verifiesIdentitySuccessfully() {
        Task task = taskWith(Map.of("name", "John Anderson", "documentType", "passport", "customerId", "CUST-1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
        assertEquals(true, result.getOutputData().get("documentAuthentic"));
        assertEquals(98.5, result.getOutputData().get("matchScore"));
        assertNotNull(result.getOutputData().get("verifiedAt"));
    }

    @Test
    void handlesNullName() {
        Map<String, Object> input = new HashMap<>();
        input.put("name", null);
        input.put("documentType", "passport");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesDifferentDocumentTypes() {
        Task task = taskWith(Map.of("name", "Jane", "documentType", "drivers_license", "customerId", "CUST-2"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("verified"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
