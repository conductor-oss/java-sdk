package roamingmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SettleWorkerTest {

    @Test
    void testSettleWorker() {
        SettleWorker worker = new SettleWorker();
        assertEquals("rmg_settle", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("homeNetwork", "US-Mobile", "visitedNetwork", "DE-Telco", "amount", "8.75"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("settled"));
    }
}
