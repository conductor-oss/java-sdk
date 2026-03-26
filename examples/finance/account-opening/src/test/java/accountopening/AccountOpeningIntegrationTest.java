package accountopening;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import accountopening.workers.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the full account opening workflow data flow.
 * Tests: collect info -> verify identity -> credit check -> open account -> welcome.
 * Also tests the rejection path.
 */
@SuppressWarnings("unchecked")
class AccountOpeningIntegrationTest {

    private final CollectInfoWorker collectWorker = new CollectInfoWorker();
    private final VerifyIdentityWorker verifyWorker = new VerifyIdentityWorker();
    private final CreditCheckWorker creditWorker = new CreditCheckWorker();
    private final OpenAccountWorker openWorker = new OpenAccountWorker();
    private final WelcomeWorker welcomeWorker = new WelcomeWorker();

    @Test
    void fullPipeline_approvedApplication() {
        // Step 1: Collect info
        TaskResult collectResult = exec(collectWorker, Map.of(
                "applicantName", "Emily Davis", "accountType", "checking",
                "initialDeposit", 1000, "ssn", "123-45-6789", "dateOfBirth", "1990-05-15"));
        assertEquals(TaskResult.Status.COMPLETED, collectResult.getStatus());
        Map<String, Object> docs = (Map<String, Object>) collectResult.getOutputData().get("documents");
        assertNotNull(docs);

        // Step 2: Verify identity (uses documents from collect)
        TaskResult verifyResult = exec(verifyWorker, Map.of(
                "applicantName", "Emily Davis", "documents", docs));
        assertEquals(TaskResult.Status.COMPLETED, verifyResult.getStatus());
        boolean verified = (boolean) verifyResult.getOutputData().get("verified");

        // Step 3: Credit check
        TaskResult creditResult = exec(creditWorker, Map.of("applicantName", "Emily Davis"));
        assertEquals(TaskResult.Status.COMPLETED, creditResult.getStatus());
        int chexScore = ((Number) creditResult.getOutputData().get("chexScore")).intValue();
        boolean eligible = (boolean) creditResult.getOutputData().get("eligible");

        // Step 4: Open account (uses verify + credit results)
        TaskResult openResult = exec(openWorker, Map.of(
                "accountType", "checking", "initialDeposit", 1000,
                "identityVerified", verified, "chexScore", chexScore));

        if (verified && eligible) {
            assertEquals(TaskResult.Status.COMPLETED, openResult.getStatus());
            String accountNumber = (String) openResult.getOutputData().get("accountNumber");
            assertTrue(accountNumber.startsWith("ACCT-"));

            // Step 5: Welcome
            TaskResult welcomeResult = exec(welcomeWorker, Map.of(
                    "applicantName", "Emily Davis", "accountNumber", accountNumber,
                    "accountType", "checking"));
            assertEquals(TaskResult.Status.COMPLETED, welcomeResult.getStatus());
            assertEquals(true, welcomeResult.getOutputData().get("welcomeSent"));
        }
    }

    @Test
    void rejectedApplication_identityNotVerified() {
        // Collect with invalid SSN -> documents.ssn = false
        TaskResult collectResult = exec(collectWorker, Map.of(
                "applicantName", "John Test", "accountType", "checking",
                "initialDeposit", 100, "ssn", "invalid-ssn", "dateOfBirth", "1985-01-01"));
        assertEquals(TaskResult.Status.COMPLETED, collectResult.getStatus());
        Map<String, Object> docs = (Map<String, Object>) collectResult.getOutputData().get("documents");
        assertEquals(false, docs.get("ssn")); // SSN validation failed

        // Verify identity -- should not verify because SSN doc is missing
        TaskResult verifyResult = exec(verifyWorker, Map.of(
                "applicantName", "John Test", "documents", docs));
        boolean verified = (boolean) verifyResult.getOutputData().get("verified");
        assertFalse(verified, "Identity should not verify with invalid SSN");

        // Open account with unverified identity -- should fail
        TaskResult openResult = exec(openWorker, Map.of(
                "accountType", "checking", "identityVerified", false,
                "chexScore", 100));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, openResult.getStatus());
        assertEquals(false, openResult.getOutputData().get("opened"));
    }

    @Test
    void auditTrailPresentAtEveryStep() {
        TaskResult collectResult = exec(collectWorker, Map.of(
                "applicantName", "Audit Test", "accountType", "checking",
                "initialDeposit", 1000, "ssn", "111-22-3333", "dateOfBirth", "1990-01-01"));
        assertNotNull(collectResult.getOutputData().get("auditTrail"));

        Map<String, Object> docs = (Map<String, Object>) collectResult.getOutputData().get("documents");
        TaskResult verifyResult = exec(verifyWorker, Map.of(
                "applicantName", "Audit Test", "documents", docs));
        assertNotNull(verifyResult.getOutputData().get("auditTrail"));

        TaskResult creditResult = exec(creditWorker, Map.of("applicantName", "Audit Test"));
        assertNotNull(creditResult.getOutputData().get("auditTrail"));

        // The audit trail should include action and timestamp
        Map<String, Object> creditAudit = (Map<String, Object>) creditResult.getOutputData().get("auditTrail");
        assertEquals("credit_check", creditAudit.get("action"));
        assertNotNull(creditAudit.get("timestamp"));
    }

    private TaskResult exec(com.netflix.conductor.client.worker.Worker worker, Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return worker.execute(task);
    }
}
