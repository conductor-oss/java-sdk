package campaignautomation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MeasureWorkerTest {
    private final MeasureWorker worker = new MeasureWorker();
    @Test void taskDefName() { assertEquals("cpa_measure", worker.getTaskDefName()); }
    @Test void measuresPerformance() {
        TaskResult r = worker.execute(taskWith(Map.of("campaignId", "CMP-1", "executionId", "EXC-1")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("245%", r.getOutputData().get("roi"));
        assertNotNull(r.getOutputData().get("metrics"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
