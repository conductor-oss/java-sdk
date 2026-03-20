package leadnurturing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SegmentWorkerTest {
    private final SegmentWorker worker = new SegmentWorker();

    @Test void taskDefName() { assertEquals("nur_segment", worker.getTaskDefName()); }

    @Test void segmentsAwareness() {
        TaskResult r = worker.execute(taskWith(Map.of("leadId", "L1", "stage", "awareness")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("top-funnel", r.getOutputData().get("segment"));
    }

    @Test void segmentsConsideration() {
        TaskResult r = worker.execute(taskWith(Map.of("leadId", "L1", "stage", "consideration")));
        assertEquals("mid-funnel", r.getOutputData().get("segment"));
    }

    @Test void segmentsDecision() {
        TaskResult r = worker.execute(taskWith(Map.of("leadId", "L1", "stage", "decision")));
        assertEquals("bottom-funnel", r.getOutputData().get("segment"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
