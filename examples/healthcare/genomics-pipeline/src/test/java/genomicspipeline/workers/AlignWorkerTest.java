package genomicspipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AlignWorkerTest {
    private final AlignWorker w = new AlignWorker();
    @Test void taskDefName() { assertEquals("gen_align", w.getTaskDefName()); }
    @Test void aligns() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("sampleId", "S1", "reads", 45000000, "referenceGenome", "GRCh38")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("alignmentFile"));
    }
}
