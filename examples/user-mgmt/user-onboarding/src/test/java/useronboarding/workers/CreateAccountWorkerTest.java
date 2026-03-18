package useronboarding.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CreateAccountWorkerTest {
    @Test void taskDefName() { assertEquals("uo_create_account", new CreateAccountWorker().getTaskDefName()); }

    @Test void createsValidAccount() {
        CreateAccountWorker w = new CreateAccountWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("username", "john_doe", "email", "john@example.com")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(((String) r.getOutputData().get("userId")).startsWith("USR-"));
    }

    @Test void rejectsInvalidEmail() {
        CreateAccountWorker w = new CreateAccountWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("username", "john_doe", "email", "not-email")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
    }
}
