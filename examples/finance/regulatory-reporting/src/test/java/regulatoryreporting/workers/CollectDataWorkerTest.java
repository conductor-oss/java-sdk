package regulatoryreporting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CollectDataWorkerTest {
    private final CollectDataWorker worker = new CollectDataWorker();
    @Test void taskDefName() { assertEquals("reg_collect_data", worker.getTaskDefName()); }
    @Test void collectsData() {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("reportType", "CALL", "reportingPeriod", "2024-Q1")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("data"));
    }
}
