package genomicspipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SequenceWorkerTest {
    private final SequenceWorker w = new SequenceWorker();
    @Test void taskDefName() { assertEquals("gen_sequence", w.getTaskDefName()); }
    @Test void sequences() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("sampleId", "S1", "panelType", "hereditary_cancer")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(45000000, r.getOutputData().get("totalReads"));
    }
}
