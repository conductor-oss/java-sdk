package numberporting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RequestWorkerTest {

    @Test
    void testRequestWorker() {
        RequestWorker worker = new RequestWorker();
        assertEquals("npt_request", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("phoneNumber", "+1-555-0172", "toCarrier", "CarrierB"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("PORT-number-porting-001", result.getOutputData().get("portId"));
    }
}
