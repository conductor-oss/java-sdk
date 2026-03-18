package accountopening.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenAccountWorkerTest {
    private final OpenAccountWorker worker = new OpenAccountWorker();

    @Test void taskDefName() { assertEquals("acc_open_account", worker.getTaskDefName()); }

    @Test void opensAccountWhenVerified() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("accountType", "checking", "initialDeposit", 1000,
                "identityVerified", true, "chexScore", 150)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("accountNumber"));
        assertTrue(((String) r.getOutputData().get("accountNumber")).startsWith("ACCT-"));
        assertEquals(true, r.getOutputData().get("opened"));
    }

    @Test void failsWhenIdentityNotVerified() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("accountType", "checking", "identityVerified", false, "chexScore", 100)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
        assertEquals(false, r.getOutputData().get("opened"));
    }

    @Test void failsWhenChexScoreTooHigh() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("accountType", "checking", "identityVerified", true, "chexScore", 800)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.FAILED, r.getStatus());
    }

    @Test void routingNumberVariesByAccountType() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("accountType", "savings", "identityVerified", true, "chexScore", 100)));
        TaskResult r = worker.execute(t);
        assertEquals("021000090", r.getOutputData().get("routingNumber"));
    }
}
