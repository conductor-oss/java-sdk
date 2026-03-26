package usageanalytics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CollectCdrsWorkerTest {

    @Test
    void testCollectCdrsWorker() {
        CollectCdrsWorker worker = new CollectCdrsWorker();
        assertEquals("uag_collect_cdrs", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("region", "WEST-COAST", "period", "2024-03"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1200000, result.getOutputData().get("cdrCount"));
    }
}
