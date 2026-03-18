package deadletterevents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DlAttemptProcessWorkerTest {
    private final DlAttemptProcessWorker worker = new DlAttemptProcessWorker();

    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("eventId", "EVT-001", "retryCount", 0,
                "maxRetries", 3, "payload", "test-data")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
