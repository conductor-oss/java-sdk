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
        t.setInputData(new HashMap<>(Map.of("verified", true, "newPassword", "MyStr0ng!Pass")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("resetSuccess"));
        assertNotNull(r.getOutputData().get("passwordHash"));
        assertFalse(((String) r.getOutputData().get("passwordHash")).isEmpty());
    }

    @Test void rejectsWhenTokenNotVerified() {
        ResetWorker w = new ResetWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("verified", false, "newPassword", "MyStr0ng!Pass")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertEquals(false, r.getOutputData().get("resetSuccess"));
    }

    @Test void rejectsWhenVerifiedMissing() {
        ResetWorker w = new ResetWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("newPassword", "MyStr0ng!Pass")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
    }

    @Test void weakPasswordFailsEvenWithVerifiedToken() {
        ResetWorker w = new ResetWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("verified", true, "newPassword", "abc")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
    }
}
