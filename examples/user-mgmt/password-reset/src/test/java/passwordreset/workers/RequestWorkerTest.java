package passwordreset.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RequestWorkerTest {
    @Test void taskDefName() { assertEquals("pwd_request", new RequestWorker().getTaskDefName()); }

    @Test void validEmailGeneratesToken() {
        RequestWorker w = new RequestWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("email", "user@example.com")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("validEmail"));
        assertNotNull(r.getOutputData().get("resetToken"));
        assertNotNull(r.getOutputData().get("userId"));
        assertNotNull(r.getOutputData().get("requestedAt"));
        assertNotNull(r.getOutputData().get("expiresAt"));
    }

    @Test void invalidEmailRejected() {
        RequestWorker w = new RequestWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("email", "not-an-email")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("Invalid email"));
    }

    @Test void missingEmailRejected() {
        RequestWorker w = new RequestWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>());
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("email"));
    }

    @Test void blankEmailRejected() {
        RequestWorker w = new RequestWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("email", "   ")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }

    @Test void tokenIsBase64UrlEncoded() {
        RequestWorker w = new RequestWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("email", "user@example.com")));
        TaskResult r = w.execute(t);
        String token = (String) r.getOutputData().get("resetToken");
        assertNotNull(token);
        assertTrue(token.length() >= 32, "Token should be at least 32 chars (256 bits base64-encoded)");
        // Should not contain characters outside URL-safe base64
        assertTrue(token.matches("[A-Za-z0-9_-]+"), "Token should be URL-safe base64");
    }
}
