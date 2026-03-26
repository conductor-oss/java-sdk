package passwordreset.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VerifyTokenWorkerTest {

    @Test
    void taskDefName() { assertEquals("pwd_verify", new VerifyTokenWorker().getTaskDefName()); }

    @Test void validTokenVerifies() {
        VerifyTokenWorker w = new VerifyTokenWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("resetToken", "abcdefghijklmnopqrstuvwxyz123456",
                "expiresAt", Instant.now().plus(1, ChronoUnit.HOURS).toString())));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("verified"));
        assertEquals(false, r.getOutputData().get("expired"));
        assertNotNull(r.getOutputData().get("verifiedAt"));
    }

    @Test void expiredTokenIsRejected() {
        VerifyTokenWorker w = new VerifyTokenWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("resetToken", "abcdefghijklmnopqrstuvwxyz123456",
                "expiresAt", Instant.now().minus(1, ChronoUnit.HOURS).toString())));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertEquals(false, r.getOutputData().get("verified"));
        assertEquals(true, r.getOutputData().get("expired"));
        assertTrue(r.getReasonForIncompletion().contains("expired"));
    }

    @Test void shortTokenIsRejected() {
        VerifyTokenWorker w = new VerifyTokenWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("resetToken", "abc",
                "expiresAt", Instant.now().plus(1, ChronoUnit.HOURS).toString())));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertEquals(false, r.getOutputData().get("verified"));
        assertTrue(r.getReasonForIncompletion().contains("invalid"));
    }

    @Test void missingTokenIsRejected() {
        VerifyTokenWorker w = new VerifyTokenWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>());
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("resetToken"));
    }

    @Test void blankTokenIsRejected() {
        VerifyTokenWorker w = new VerifyTokenWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("resetToken", "   ")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void userIdIsPreservedInOutput() {
        VerifyTokenWorker w = new VerifyTokenWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of(
                "resetToken", "abcdefghijklmnopqrstuvwxyz123456",
                "expiresAt", Instant.now().plus(1, ChronoUnit.HOURS).toString(),
                "userId", "USR-ABC123")));
        TaskResult r = w.execute(t);
        assertEquals("USR-ABC123", r.getOutputData().get("userId"));
    }
}
