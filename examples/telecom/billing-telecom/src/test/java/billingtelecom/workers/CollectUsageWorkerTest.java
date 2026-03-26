package billingtelecom.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CollectUsageWorkerTest {

    @Test
    void testCollectUsageWorker() {
        CollectUsageWorker worker = new CollectUsageWorker();
        assertEquals("btl_collect_usage", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("customerId", "CUST-811", "billingPeriod", "2024-03"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("usageRecords"));
    }
}
