package passwordreset;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import passwordreset.workers.NotifyWorker;
import passwordreset.workers.RequestWorker;
import passwordreset.workers.ResetWorker;
import passwordreset.workers.VerifyTokenWorker;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying worker-to-worker data flow in the password reset pipeline.
 * Tests the full: Request -> Verify -> Reset -> Notify pipeline.
 */
class PasswordResetIntegrationTest {

    @Test
    void fullSuccessfulResetPipeline() {
        // Step 1: Request reset
        RequestWorker requestWorker = new RequestWorker();
        Task requestTask = new Task();
        requestTask.setStatus(Task.Status.IN_PROGRESS);
        requestTask.setInputData(new HashMap<>(Map.of("email", "alice@example.com")));
        TaskResult requestResult = requestWorker.execute(requestTask);
        assertEquals(TaskResult.Status.COMPLETED, requestResult.getStatus());

        String resetToken = (String) requestResult.getOutputData().get("resetToken");
        String expiresAt = (String) requestResult.getOutputData().get("expiresAt");
        String userId = (String) requestResult.getOutputData().get("userId");
        assertNotNull(resetToken);
        assertNotNull(expiresAt);
        assertNotNull(userId);

        // Step 2: Verify token
        VerifyTokenWorker verifyWorker = new VerifyTokenWorker();
        Task verifyTask = new Task();
        verifyTask.setStatus(Task.Status.IN_PROGRESS);
        verifyTask.setInputData(new HashMap<>(Map.of(
                "resetToken", resetToken,
                "expiresAt", expiresAt,
                "userId", userId
        )));
        TaskResult verifyResult = verifyWorker.execute(verifyTask);
        assertEquals(TaskResult.Status.COMPLETED, verifyResult.getStatus());
        assertEquals(true, verifyResult.getOutputData().get("verified"));

        // Step 3: Reset password
        ResetWorker resetWorker = new ResetWorker();
        Task resetTask = new Task();
        resetTask.setStatus(Task.Status.IN_PROGRESS);
        resetTask.setInputData(new HashMap<>(Map.of(
                "verified", verifyResult.getOutputData().get("verified"),
                "newPassword", "MyStr0ng!Pass",
                "userId", userId
        )));
        TaskResult resetResult = resetWorker.execute(resetTask);
        assertEquals(TaskResult.Status.COMPLETED, resetResult.getStatus());
        assertEquals(true, resetResult.getOutputData().get("resetSuccess"));
        assertNotNull(resetResult.getOutputData().get("passwordHash"));
        assertNotNull(resetResult.getOutputData().get("salt"));
        assertEquals(userId, resetResult.getOutputData().get("userId"));

        // Step 4: Notify
        NotifyWorker notifyWorker = new NotifyWorker();
        Task notifyTask = new Task();
        notifyTask.setStatus(Task.Status.IN_PROGRESS);
        notifyTask.setInputData(new HashMap<>(Map.of(
                "email", "alice@example.com",
                "resetSuccess", resetResult.getOutputData().get("resetSuccess"),
                "userId", userId
        )));
        TaskResult notifyResult = notifyWorker.execute(notifyTask);
        assertEquals(TaskResult.Status.COMPLETED, notifyResult.getStatus());
        assertEquals("Your password has been reset", notifyResult.getOutputData().get("emailSubject"));
        assertEquals(userId, notifyResult.getOutputData().get("userId"));
    }

    @Test
    void expiredTokenBlocksEntirePipeline() {
        // Step 1: Request (simulate an already-expired token)
        VerifyTokenWorker verifyWorker = new VerifyTokenWorker();
        Task verifyTask = new Task();
        verifyTask.setStatus(Task.Status.IN_PROGRESS);
        verifyTask.setInputData(new HashMap<>(Map.of(
                "resetToken", "abcdefghijklmnopqrstuvwxyz123456",
                "expiresAt", Instant.now().minus(1, ChronoUnit.HOURS).toString()
        )));
        TaskResult verifyResult = verifyWorker.execute(verifyTask);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, verifyResult.getStatus());
        assertEquals(true, verifyResult.getOutputData().get("expired"));

        // Step 2: Reset should not proceed — if it were called with verified=false, it fails
        ResetWorker resetWorker = new ResetWorker();
        Task resetTask = new Task();
        resetTask.setStatus(Task.Status.IN_PROGRESS);
        resetTask.setInputData(new HashMap<>(Map.of(
                "verified", false,
                "newPassword", "MyStr0ng!Pass"
        )));
        TaskResult resetResult = resetWorker.execute(resetTask);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, resetResult.getStatus());
    }

    @Test
    void weakPasswordBlocksResetButNotNotification() {
        // Verify succeeds
        VerifyTokenWorker verifyWorker = new VerifyTokenWorker();
        Task verifyTask = new Task();
        verifyTask.setStatus(Task.Status.IN_PROGRESS);
        verifyTask.setInputData(new HashMap<>(Map.of(
                "resetToken", "abcdefghijklmnopqrstuvwxyz123456",
                "expiresAt", Instant.now().plus(1, ChronoUnit.HOURS).toString()
        )));
        TaskResult verifyResult = verifyWorker.execute(verifyTask);
        assertEquals(TaskResult.Status.COMPLETED, verifyResult.getStatus());

        // Reset fails due to weak password
        ResetWorker resetWorker = new ResetWorker();
        Task resetTask = new Task();
        resetTask.setStatus(Task.Status.IN_PROGRESS);
        resetTask.setInputData(new HashMap<>(Map.of(
                "verified", true,
                "newPassword", "weak"
        )));
        TaskResult resetResult = resetWorker.execute(resetTask);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, resetResult.getStatus());

        // Notify should still work (to inform user of failure)
        NotifyWorker notifyWorker = new NotifyWorker();
        Task notifyTask = new Task();
        notifyTask.setStatus(Task.Status.IN_PROGRESS);
        notifyTask.setInputData(new HashMap<>(Map.of(
                "email", "alice@example.com",
                "resetSuccess", false
        )));
        TaskResult notifyResult = notifyWorker.execute(notifyTask);
        assertEquals(TaskResult.Status.COMPLETED, notifyResult.getStatus());
        assertEquals("Password reset failed", notifyResult.getOutputData().get("emailSubject"));
    }

    @Test
    void auditTrailPreservedAcrossPipeline() {
        // Request
        RequestWorker requestWorker = new RequestWorker();
        Task requestTask = new Task();
        requestTask.setStatus(Task.Status.IN_PROGRESS);
        requestTask.setInputData(new HashMap<>(Map.of("email", "audit@example.com")));
        TaskResult requestResult = requestWorker.execute(requestTask);
        String userId = (String) requestResult.getOutputData().get("userId");

        // Reset with known userId
        ResetWorker resetWorker = new ResetWorker();
        Task resetTask = new Task();
        resetTask.setStatus(Task.Status.IN_PROGRESS);
        resetTask.setInputData(new HashMap<>(Map.of(
                "verified", true,
                "newPassword", "Audit!Test1",
                "userId", userId
        )));
        TaskResult resetResult = resetWorker.execute(resetTask);
        assertEquals(TaskResult.Status.COMPLETED, resetResult.getStatus());

        // Verify audit fields are present
        assertEquals(userId, resetResult.getOutputData().get("userId"));
        assertNotNull(resetResult.getOutputData().get("resetAt"));
        assertTrue(((Number) resetResult.getOutputData().get("strength")).intValue() > 0);
    }
}
