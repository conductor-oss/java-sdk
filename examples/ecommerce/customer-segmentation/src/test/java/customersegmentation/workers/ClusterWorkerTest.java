package customersegmentation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ClusterWorkerTest {
    private final ClusterWorker worker = new ClusterWorker();
    @Test void taskDefName() { assertEquals("seg_cluster", worker.getTaskDefName()); }
    @Test void returnsClusters() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("customers", List.of(), "numSegments", 3)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("clusters") instanceof List);
    }
}
