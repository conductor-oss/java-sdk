package genomicspipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AnnotateWorkerTest {
    private final AnnotateWorker w = new AnnotateWorker();
    @Test void taskDefName() { assertEquals("gen_annotate", w.getTaskDefName()); }
    @Test void annotates() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        List<Map<String, Object>> variants = new ArrayList<>();
        variants.add(new HashMap<>(Map.of("gene", "BRCA1", "type", "SNV")));
        t.setInputData(new HashMap<>(Map.of("sampleId", "S1", "variants", variants)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("annotatedVariants"));
    }
}
