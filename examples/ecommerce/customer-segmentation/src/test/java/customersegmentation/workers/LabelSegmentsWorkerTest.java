package customersegmentation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LabelSegmentsWorkerTest {
    private final LabelSegmentsWorker worker = new LabelSegmentsWorker();
    @Test void taskDefName() { assertEquals("seg_label_segments", worker.getTaskDefName()); }
    @Test void returnsSegments() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("clusters", List.of())));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("segments") instanceof List);
    }
}
