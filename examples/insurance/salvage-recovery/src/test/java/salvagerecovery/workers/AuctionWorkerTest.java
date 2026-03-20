package salvagerecovery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AuctionWorkerTest {

    @Test
    void testAuctionWorker() {
        AuctionWorker worker = new AuctionWorker();
        assertEquals("slv_auction", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("vehicleId", "VIN-98321", "reservePrice", "4200"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5100, result.getOutputData().get("proceeds"));
    }
}
