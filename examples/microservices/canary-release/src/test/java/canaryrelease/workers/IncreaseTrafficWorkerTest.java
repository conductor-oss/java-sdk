package canaryrelease.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class IncreaseTrafficWorkerTest {
    private final IncreaseTrafficWorker worker = new IncreaseTrafficWorker();

    @Test void taskDefName() { assertEquals("cy_increase_traffic", worker.getTaskDefName()); }

    @Test void increasesTraffic() {
        Task task = taskWith(Map.of("trafficPercent", 50));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(50, r.getOutputData().get("trafficPercent"));
        assertEquals(5, r.getOutputData().get("canaryInstances"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
