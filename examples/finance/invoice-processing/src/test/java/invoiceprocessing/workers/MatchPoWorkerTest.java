package invoiceprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MatchPoWorkerTest {
    private final MatchPoWorker worker = new MatchPoWorker();

    @Test void taskDefName() { assertEquals("ivc_match_po", worker.getTaskDefName()); }

    @Test void computesVariance() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("extractedPoNumber", "PO-1234", "extractedAmount", 8750)));
        TaskResult r = worker.execute(t);
        assertNotNull(r.getOutputData().get("variance"));
        assertNotNull(r.getOutputData().get("variancePercent"));
        assertTrue(r.getOutputData().get("matched") instanceof Boolean);
    }
}
