package legalcontractreview.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LcrExtractTermsWorkerTest {
    @Test void taskDefName() { assertEquals("lcr_extract_terms", new LcrExtractTermsWorker().getTaskDefName()); }

    @Test void extractsTerms() {
        LcrExtractTermsWorker w = new LcrExtractTermsWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("contractText", "Standard contract", "liability", "Unlimited")));
        TaskResult r = w.execute(t);
        assertNotNull(r.getOutputData().get("keyTerms"));
        @SuppressWarnings("unchecked")
        List<String> flags = (List<String>) r.getOutputData().get("riskFlags");
        assertTrue(flags.stream().anyMatch(f -> f.contains("Unlimited")));
    }
}
