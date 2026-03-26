package secretrotation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class StoreSecretWorkerTest {
    private final StoreSecretWorker worker = new StoreSecretWorker();

    @Test void taskDefName() { assertEquals("sr_store_secret", worker.getTaskDefName()); }

    @Test void completesSuccessfully() {
        Task t = new Task();
        t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("secretName", "db-password", "secretId", "sec-1", "algorithm", "AES-256", "vault", "vault", "targetServices", List.of("api"))));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }

    @Test void handlesMissingInputs() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>());
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(t).getStatus());
    }
}
