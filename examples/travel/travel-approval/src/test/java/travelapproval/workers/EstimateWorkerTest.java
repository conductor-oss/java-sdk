package travelapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EstimateWorkerTest {
    private final EstimateWorker worker = new EstimateWorker();

    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("requestId", "TR-001", "destination", "London",
                "estimatedCost", 2500, "amount", 2500, "threshold", 5000)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
