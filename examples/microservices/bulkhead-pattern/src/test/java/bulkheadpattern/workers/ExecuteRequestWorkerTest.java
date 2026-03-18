package bulkheadpattern.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ExecuteRequestWorkerTest {
    @Test void executesSuccessfully() {
        ExecuteRequestWorker w = new ExecuteRequestWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("serviceName", "order-service", "priority", "high",
                "pool", "premium-pool", "maxConcurrency", 50, "poolId", "POOL-123")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
