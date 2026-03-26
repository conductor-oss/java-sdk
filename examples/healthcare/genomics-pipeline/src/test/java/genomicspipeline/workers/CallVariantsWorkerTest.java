package genomicspipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CallVariantsWorkerTest {
    private final CallVariantsWorker w = new CallVariantsWorker();
    @Test void taskDefName() { assertEquals("gen_call_variants", w.getTaskDefName()); }
    @Test void callsVariants() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("sampleId", "S1", "alignmentFile", "aligned.bam")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(3, r.getOutputData().get("totalVariants"));
    }
}
