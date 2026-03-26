package passwordreset.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ResetWorkerTest {
    @Test void taskDefName() { assertEquals("pwd_reset", new ResetWorker().getTaskDefName()); }

    @Test void strongPasswordWithVerifiedTokenSucceeds() {
        ResetWorker w = new ResetWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("verified", true, "newPassword", "MyStr0ng!Pass", "userId", "USR-123")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("resetSuccess"));
        assertNotNull(r.getOutputData().get("passwordHash"));
        assertFalse(((String) r.getOutputData().get("passwordHash")).isEmpty());
        assertNotNull(r.getOutputData().get("salt"));
        assertNotNull(r.getOutputData().get("resetAt"));
        assertEquals("USR-123", r.getOutputData().get("userId"));
        assertTrue(((Number) r.getOutputData().get("strength")).intValue() >= 3);
    }

    @Test void rejectsWhenTokenNotVerified() {
        ResetWorker w = new ResetWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("verified", false, "newPassword", "MyStr0ng!Pass")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertEquals(false, r.getOutputData().get("resetSuccess"));
        assertNotNull(r.getOutputData().get("resetAt"), "Audit field 'resetAt' must be present even on failure");
        assertNotNull(r.getOutputData().get("userId"), "Audit field 'userId' must be present even on failure");
    }

    @Test void rejectsWhenVerifiedMissing() {
        ResetWorker w = new ResetWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("newPassword", "MyStr0ng!Pass")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void weakPasswordTooShort() {
        ResetWorker w = new ResetWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("verified", true, "newPassword", "abc")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("weak"));
        assertNotNull(r.getOutputData().get("strength"));
        assertNotNull(r.getOutputData().get("resetAt"));
    }

    @Test void weakPasswordNoSpecialChars() {
        ResetWorker w = new ResetWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("verified", true, "newPassword", "abcdefgh")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("weak"));
    }

    @Test void weakPasswordAllLowercase() {
        ResetWorker w = new ResetWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("verified", true, "newPassword", "alllowercase")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void blankPasswordRejected() {
        ResetWorker w = new ResetWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("verified", true, "newPassword", "")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("newPassword"));
    }

    @Test void auditFieldsPresentOnEveryOutcome() {
        ResetWorker w = new ResetWorker();

        // Successful case
        Task t1 = new Task(); t1.setStatus(Task.Status.IN_PROGRESS);
        t1.setInputData(new HashMap<>(Map.of("verified", true, "newPassword", "MyStr0ng!Pass", "userId", "USR-1")));
        TaskResult r1 = w.execute(t1);
        assertNotNull(r1.getOutputData().get("resetAt"), "resetAt must be present on success");
        assertNotNull(r1.getOutputData().get("userId"), "userId must be present on success");
        assertNotNull(r1.getOutputData().get("strength"), "strength must be present on success");

        // Failure case - weak password
        Task t2 = new Task(); t2.setStatus(Task.Status.IN_PROGRESS);
        t2.setInputData(new HashMap<>(Map.of("verified", true, "newPassword", "weak", "userId", "USR-2")));
        TaskResult r2 = w.execute(t2);
        assertNotNull(r2.getOutputData().get("resetAt"), "resetAt must be present on failure");
        assertNotNull(r2.getOutputData().get("userId"), "userId must be present on failure");
        assertNotNull(r2.getOutputData().get("strength"), "strength must be present on failure");

        // Failure case - unverified token
        Task t3 = new Task(); t3.setStatus(Task.Status.IN_PROGRESS);
        t3.setInputData(new HashMap<>(Map.of("verified", false, "newPassword", "MyStr0ng!Pass", "userId", "USR-3")));
        TaskResult r3 = w.execute(t3);
        assertNotNull(r3.getOutputData().get("resetAt"), "resetAt must be present on unverified");
        assertNotNull(r3.getOutputData().get("userId"), "userId must be present on unverified");
        assertNotNull(r3.getOutputData().get("strength"), "strength must be present on unverified");
    }
}
