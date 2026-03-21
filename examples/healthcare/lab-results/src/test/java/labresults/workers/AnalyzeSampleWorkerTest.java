package labresults.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class AnalyzeSampleWorkerTest {
    private final AnalyzeSampleWorker w = new AnalyzeSampleWorker();
    @Test void taskDefName() { assertEquals("lab_analyze", w.getTaskDefName()); }
    @Test void returnsResults() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("sampleId", "SMP-1", "processedData", Map.of())));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(r.getOutputData().get("results") instanceof Map);
        assertEquals(false, r.getOutputData().get("critical"));
    }
}
