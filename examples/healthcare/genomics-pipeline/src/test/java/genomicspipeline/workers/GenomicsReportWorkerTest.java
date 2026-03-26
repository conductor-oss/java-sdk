package genomicspipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GenomicsReportWorkerTest {
    private final GenomicsReportWorker w = new GenomicsReportWorker();
    @Test void taskDefName() { assertEquals("gen_report", w.getTaskDefName()); }
    @Test void generatesReport() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        List<Map<String, Object>> ann = new ArrayList<>();
        ann.add(new HashMap<>(Map.of("gene", "BRCA1", "clinicalSignificance", "pathogenic")));
        t.setInputData(new HashMap<>(Map.of("sampleId", "S1", "patientId", "P1", "annotatedVariants", ann)));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(1, r.getOutputData().get("clinicallySignificant"));
    }
}
