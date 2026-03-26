package gdprcompliance;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import gdprcompliance.workers.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MainExampleTest {

    // --- ProcessRequestWorker: PII detection ---

    @Test void detectsEmailPII() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = taskWith(Map.of("requestType", "access", "data", "Contact john@example.com for info"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(((Number) r.getOutputData().get("piiCount")).intValue() >= 1);
    }

    @Test void detectsSSNPII() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = taskWith(Map.of("requestType", "access", "data", "SSN is 123-45-6789"));
        TaskResult r = w.execute(t);
        assertTrue(((Number) r.getOutputData().get("piiCount")).intValue() >= 1);
    }

    @Test void detectsUSPhoneNumber() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = taskWith(Map.of("requestType", "access", "data", "Call 555-123-4567"));
        TaskResult r = w.execute(t);
        assertTrue(((Number) r.getOutputData().get("piiCount")).intValue() >= 1);
    }

    @Test void detectsInternationalPhoneFormat() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = taskWith(Map.of("requestType", "access", "data", "Call +44.207.123.4567 for help"));
        TaskResult r = w.execute(t);
        assertTrue(((Number) r.getOutputData().get("piiCount")).intValue() >= 1);
    }

    @Test void detectsCreditCard() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = taskWith(Map.of("requestType", "access", "data", "Card: 4111-1111-1111-1111"));
        TaskResult r = w.execute(t);
        assertTrue(((Number) r.getOutputData().get("piiCount")).intValue() >= 1);
    }

    @Test void detectsMultiplePIITypes() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = taskWith(Map.of("requestType", "access",
                "data", "john@test.com SSN: 123-45-6789 Phone: 555-123-4567 Card: 4111 1111 1111 1111"));
        TaskResult r = w.execute(t);
        assertTrue(((Number) r.getOutputData().get("piiCount")).intValue() >= 4);
    }

    @Test void noPIIInCleanData() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = taskWith(Map.of("requestType", "access", "data", "This is clean text with no PII at all."));
        TaskResult r = w.execute(t);
        assertEquals(0, ((Number) r.getOutputData().get("piiCount")).intValue());
    }

    @Test void partialSSNNotDetected() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        // Only two digits in first group -- not a valid SSN pattern
        Task t = taskWith(Map.of("requestType", "access", "data", "Partial 12-45-6789 is not SSN"));
        TaskResult r = w.execute(t);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> pii = (List<Map<String, String>>) r.getOutputData().get("piiFound");
        boolean hasSsn = pii.stream().anyMatch(p -> "ssn".equals(p.get("type")));
        assertFalse(hasSsn, "Partial SSN should not be detected");
    }

    @Test void masksDataOnErasure() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = taskWith(Map.of("requestType", "erasure",
                "data", "Email: test@example.com, SSN: 123-45-6789"));
        TaskResult r = w.execute(t);
        String processed = (String) r.getOutputData().get("processedData");
        assertFalse(processed.contains("test@example.com"));
        assertFalse(processed.contains("123-45-6789"));
        assertTrue(processed.contains("[EMAIL_REDACTED]"));
        assertTrue(processed.contains("[SSN_REDACTED]"));
    }

    @Test void masksDataOnAnonymize() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = taskWith(Map.of("requestType", "anonymize",
                "data", "Phone: 555-123-4567"));
        TaskResult r = w.execute(t);
        String processed = (String) r.getOutputData().get("processedData");
        assertFalse(processed.contains("555-123-4567"));
    }

    @Test void accessDoesNotMaskData() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = taskWith(Map.of("requestType", "access",
                "data", "Email: test@example.com"));
        TaskResult r = w.execute(t);
        String processed = (String) r.getOutputData().get("processedData");
        assertTrue(processed.contains("test@example.com"), "Access request should not mask data");
    }

    @Test void processRequestFailsOnMissingRequestType() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = taskWith(Map.of("data", "some data"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void processRequestFailsOnInvalidRequestType() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = taskWith(Map.of("requestType", "delete_everything", "data", "text"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void processRequestFailsOnMissingData() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = taskWith(Map.of("requestType", "access"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void processRequestIncludesAuditLog() {
        ProcessRequestWorker w = new ProcessRequestWorker();
        Task t = taskWith(Map.of("requestType", "access", "data", "clean text"));
        TaskResult r = w.execute(t);
        assertNotNull(r.getOutputData().get("auditLog"));
        @SuppressWarnings("unchecked")
        Map<String, String> log = (Map<String, String>) r.getOutputData().get("auditLog");
        assertNotNull(log.get("timestamp"));
        assertNotNull(log.get("action"));
        assertNotNull(log.get("actor"));
        assertNotNull(log.get("result"));
    }

    // --- VerifyIdentityWorker ---

    @Test void verifyIdentitySucceeds() {
        VerifyIdentityWorker w = new VerifyIdentityWorker();
        Task t = taskWith(Map.of("requestorId", "USER-123", "email", "user@example.com"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("verified"));
    }

    @Test void verifyIdentityFailsOnMissingRequestorId() {
        VerifyIdentityWorker w = new VerifyIdentityWorker();
        Task t = taskWith(Map.of("email", "user@example.com"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void verifyIdentityFailsOnMissingEmail() {
        VerifyIdentityWorker w = new VerifyIdentityWorker();
        Task t = taskWith(Map.of("requestorId", "USER-123"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void verifyIdentityFailsOnInvalidEmail() {
        VerifyIdentityWorker w = new VerifyIdentityWorker();
        Task t = taskWith(Map.of("requestorId", "USER-123", "email", "not-an-email"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void verifyIdentityIncludesAuditLog() {
        VerifyIdentityWorker w = new VerifyIdentityWorker();
        Task t = taskWith(Map.of("requestorId", "USER-123", "email", "user@example.com"));
        TaskResult r = w.execute(t);
        @SuppressWarnings("unchecked")
        Map<String, String> log = (Map<String, String>) r.getOutputData().get("auditLog");
        assertEquals("gdpr_verify_identity", log.get("action"));
        assertEquals("USER-123", log.get("actor"));
        assertEquals("SUCCESS", log.get("result"));
    }

    // --- LocateDataWorker ---

    @Test void locateDataFailsOnMissingSubjectId() {
        LocateDataWorker w = new LocateDataWorker();
        Task t = taskWith(Map.of("dataSources", List.of()));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void locateDataFailsOnMissingDataSources() {
        LocateDataWorker w = new LocateDataWorker();
        Task t = taskWith(Map.of("subjectId", "USER-123"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("dataSources"));
    }

    @Test void locateDataScansRealRecords() {
        LocateDataWorker w = new LocateDataWorker();
        List<Map<String, Object>> dataSources = List.of(
                Map.of("system", "user_db", "table", "users",
                        "fields", List.of("name", "email", "userId"),
                        "records", List.of(
                                Map.of("userId", "USER-123", "name", "John", "email", "john@test.com"),
                                Map.of("userId", "USER-456", "name", "Jane", "email", "jane@test.com")
                        )),
                Map.of("system", "logs_db", "table", "events",
                        "fields", List.of("userId", "event"),
                        "records", List.of(
                                Map.of("userId", "USER-789", "event", "login")
                        ))
        );
        Task t = taskWith(Map.of("subjectId", "USER-123", "dataSources", dataSources));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(2, r.getOutputData().get("systemsScanned"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> locations = (List<Map<String, Object>>) r.getOutputData().get("dataLocations");
        assertEquals(1, locations.size());
        assertEquals("user_db", locations.get(0).get("system"));
    }

    @Test void locateDataNoMatchReturnsEmpty() {
        LocateDataWorker w = new LocateDataWorker();
        List<Map<String, Object>> dataSources = List.of(
                Map.of("system", "user_db", "table", "users",
                        "fields", List.of("name"),
                        "records", List.of(Map.of("name", "unrelated")))
        );
        Task t = taskWith(Map.of("subjectId", "USER-999", "dataSources", dataSources));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        @SuppressWarnings("unchecked")
        List<?> locations = (List<?>) r.getOutputData().get("dataLocations");
        assertEquals(0, locations.size());
    }

    // --- ConfirmCompletionWorker ---

    @Test void confirmCompletionSucceeds() {
        ConfirmCompletionWorker w = new ConfirmCompletionWorker();
        Task t = taskWith(Map.of("requestType", "erasure", "piiCount", 3, "subjectId", "USER-123"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("completed"));
        assertTrue(r.getOutputData().get("reportId").toString().startsWith("GDPR-"));
        assertEquals(3, r.getOutputData().get("piiItemsProcessed"));
    }

    @Test void confirmCompletionFailsOnMissingRequestType() {
        ConfirmCompletionWorker w = new ConfirmCompletionWorker();
        Task t = taskWith(Map.of("piiCount", 0, "subjectId", "USER-123"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void confirmCompletionFailsOnMissingSubjectId() {
        ConfirmCompletionWorker w = new ConfirmCompletionWorker();
        Task t = taskWith(Map.of("requestType", "access", "piiCount", 0));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void confirmCompletionFailsOnMissingPiiCount() {
        ConfirmCompletionWorker w = new ConfirmCompletionWorker();
        Task t = taskWith(Map.of("requestType", "access", "subjectId", "USER-123"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void confirmCompletionFailsOnNegativePiiCount() {
        ConfirmCompletionWorker w = new ConfirmCompletionWorker();
        Task t = taskWith(Map.of("requestType", "access", "piiCount", -1, "subjectId", "USER-123"));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void confirmCompletionIncludesAuditLog() {
        ConfirmCompletionWorker w = new ConfirmCompletionWorker();
        Task t = taskWith(Map.of("requestType", "access", "piiCount", 0, "subjectId", "USER-123"));
        TaskResult r = w.execute(t);
        @SuppressWarnings("unchecked")
        Map<String, String> log = (Map<String, String>) r.getOutputData().get("auditLog");
        assertEquals("gdpr_confirm_completion", log.get("action"));
        assertEquals("USER-123", log.get("actor"));
        assertEquals("SUCCESS", log.get("result"));
    }

    // --- Helper ---

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
