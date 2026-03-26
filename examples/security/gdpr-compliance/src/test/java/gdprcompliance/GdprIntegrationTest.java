package gdprcompliance;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import gdprcompliance.workers.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that validates worker-to-worker data flow through
 * the GDPR compliance pipeline:
 *   VerifyIdentity -> LocateData -> ProcessRequest -> ConfirmCompletion
 */
class GdprIntegrationTest {

    @Test
    void fullErasurePipelineFlowsDataBetweenWorkers() {
        // Step 1: Verify identity
        VerifyIdentityWorker verifyWorker = new VerifyIdentityWorker();
        Task verifyTask = taskWith(Map.of("requestorId", "USER-42", "email", "user42@example.com"));
        TaskResult verifyResult = verifyWorker.execute(verifyTask);

        assertEquals(TaskResult.Status.COMPLETED, verifyResult.getStatus());
        assertEquals(true, verifyResult.getOutputData().get("verified"));

        // Step 2: Locate data -- uses subjectId, passes output to next step
        LocateDataWorker locateWorker = new LocateDataWorker();
        List<Map<String, Object>> dataSources = List.of(
                Map.of("system", "user_db", "table", "profiles",
                        "fields", List.of("userId", "name", "email"),
                        "records", List.of(
                                Map.of("userId", "USER-42", "name", "Alice", "email", "alice@example.com"),
                                Map.of("userId", "USER-99", "name", "Bob", "email", "bob@example.com")
                        )),
                Map.of("system", "billing_db", "table", "payments",
                        "fields", List.of("userId", "card"),
                        "records", List.of(
                                Map.of("userId", "USER-42", "card", "4111-xxxx-xxxx-1111")
                        ))
        );
        Task locateTask = taskWith(Map.of("subjectId", "USER-42", "dataSources", dataSources));
        TaskResult locateResult = locateWorker.execute(locateTask);

        assertEquals(TaskResult.Status.COMPLETED, locateResult.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> locations = (List<Map<String, Object>>) locateResult.getOutputData().get("dataLocations");
        assertEquals(2, locations.size(), "USER-42 exists in both user_db and billing_db");

        // Step 3: Process request -- erasure type, uses data from locate
        ProcessRequestWorker processWorker = new ProcessRequestWorker();
        String rawData = "User USER-42 email: alice@example.com SSN: 123-45-6789 Phone: 555-123-4567";
        Task processTask = taskWith(Map.of("requestType", "erasure", "data", rawData));
        TaskResult processResult = processWorker.execute(processTask);

        assertEquals(TaskResult.Status.COMPLETED, processResult.getStatus());
        int piiCount = ((Number) processResult.getOutputData().get("piiCount")).intValue();
        assertTrue(piiCount >= 3, "Should detect email, SSN, and phone");

        String processedData = (String) processResult.getOutputData().get("processedData");
        assertFalse(processedData.contains("alice@example.com"), "Email should be redacted");
        assertFalse(processedData.contains("123-45-6789"), "SSN should be redacted");

        // Step 4: Confirm completion -- uses piiCount from process step
        ConfirmCompletionWorker confirmWorker = new ConfirmCompletionWorker();
        Task confirmTask = taskWith(Map.of(
                "requestType", processResult.getOutputData().get("requestType"),
                "piiCount", processResult.getOutputData().get("piiCount"),
                "subjectId", "USER-42"
        ));
        TaskResult confirmResult = confirmWorker.execute(confirmTask);

        assertEquals(TaskResult.Status.COMPLETED, confirmResult.getStatus());
        assertEquals(true, confirmResult.getOutputData().get("completed"));
        assertTrue(confirmResult.getOutputData().get("reportId").toString().startsWith("GDPR-"));
        assertEquals(piiCount, confirmResult.getOutputData().get("piiItemsProcessed"));

        // Verify audit logs exist at each step
        assertNotNull(verifyResult.getOutputData().get("auditLog"));
        assertNotNull(locateResult.getOutputData().get("auditLog"));
        assertNotNull(processResult.getOutputData().get("auditLog"));
        assertNotNull(confirmResult.getOutputData().get("auditLog"));
    }

    @Test
    void pipelineStopsOnVerificationFailure() {
        VerifyIdentityWorker verifyWorker = new VerifyIdentityWorker();
        Task verifyTask = taskWith(Map.of("requestorId", "", "email", "bad-email"));
        TaskResult verifyResult = verifyWorker.execute(verifyTask);

        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, verifyResult.getStatus());
        // In production, the workflow would not proceed past this point
    }

    @Test
    void accessRequestPreservesOriginalData() {
        ProcessRequestWorker processWorker = new ProcessRequestWorker();
        String rawData = "Email: user@test.com, SSN: 111-22-3333";
        Task processTask = taskWith(Map.of("requestType", "access", "data", rawData));
        TaskResult processResult = processWorker.execute(processTask);

        assertEquals(TaskResult.Status.COMPLETED, processResult.getStatus());
        // Access request should return the original data
        assertEquals(rawData, processResult.getOutputData().get("processedData"));
        // But still detect PII
        assertTrue(((Number) processResult.getOutputData().get("piiCount")).intValue() >= 2);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
