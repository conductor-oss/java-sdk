package passwordreset.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NotifyWorkerTest {
    @Test void taskDefName() { assertEquals("pwd_notify", new NotifyWorker().getTaskDefName()); }

    @Test void notifiesSuccessOnResetSuccess() {
        NotifyWorker w = new NotifyWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("email", "user@test.com", "resetSuccess", true)));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("notified"));
        assertEquals("Your password has been reset", r.getOutputData().get("emailSubject"));
    }

    @Test void notifiesFailureWhenResetFailed() {
        NotifyWorker w = new NotifyWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("email", "user@test.com", "resetSuccess", false)));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("notified"));
        assertEquals("Password reset failed", r.getOutputData().get("emailSubject"));
    }

    @Test void defaultsToFailureWhenResetSuccessMissing() {
        NotifyWorker w = new NotifyWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("email", "user@test.com")));
        TaskResult r = w.execute(t);
        assertEquals("Password reset failed", r.getOutputData().get("emailSubject"));
    }
}
