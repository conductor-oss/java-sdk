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
        assertEquals(true, r.getOutputData().get("validEmail"));
        assertNotNull(r.getOutputData().get("resetToken"));
    }

    @Test void invalidEmailFlagged() {
        RequestWorker w = new RequestWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("email", "not-an-email")));
        TaskResult r = w.execute(t);
        assertEquals(false, r.getOutputData().get("validEmail"));
    }
}
