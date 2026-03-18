package canarydeployment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ShiftTrafficWorkerTest {
    @Test void executesSuccessfully() {
        ShiftTrafficWorker w = new ShiftTrafficWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("serviceName", "api-service", "newVersion", "2.1.0",
                "percentage", 10, "duration", "5m", "errorRate", 0.3, "threshold", 1.0)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
