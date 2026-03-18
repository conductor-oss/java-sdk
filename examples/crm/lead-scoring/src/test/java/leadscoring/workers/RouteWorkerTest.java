package leadscoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RouteWorkerTest {
    @Test void routesHotLead() {
        RouteWorker w = new RouteWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("classification", "hot", "priority", "P1")));
        TaskResult r = w.execute(t);
        assertEquals("Senior AE Team", r.getOutputData().get("assignedTo"));
    }
}
