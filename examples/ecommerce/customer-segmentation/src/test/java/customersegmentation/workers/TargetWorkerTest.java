package customersegmentation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TargetWorkerTest {
    private final TargetWorker worker = new TargetWorker();
    @Test void taskDefName() { assertEquals("seg_target", worker.getTaskDefName()); }
    @Test void returnsCampaigns() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("segments", List.of())));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("campaigns") instanceof List);
    }
}
