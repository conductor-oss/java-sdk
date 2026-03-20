package portfoliorebalancing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AnalyzeDriftWorkerTest {
    private final AnalyzeDriftWorker worker = new AnalyzeDriftWorker();

    @Test void taskDefName() { assertEquals("prt_analyze_drift", worker.getTaskDefName()); }

    @Test void analyzesDriftSuccessfully() {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("portfolioId", "PORT-1", "strategy", "60_20_15_5")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("driftAnalysis"));
        assertEquals(true, result.getOutputData().get("rebalanceNeeded"));
    }
}
