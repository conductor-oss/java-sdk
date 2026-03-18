package useronboarding.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class WelcomeWorkerTest {
    @Test void sendsWelcome() {
        WelcomeWorker w = new WelcomeWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("username", "john", "userId", "USR-123")));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("welcomeSent"));
        assertTrue(((String)r.getOutputData().get("emailBody")).contains("john"));
    }
}
