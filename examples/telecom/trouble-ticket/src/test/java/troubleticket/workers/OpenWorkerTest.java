package troubleticket.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OpenWorkerTest {

    @Test
    void testOpenWorker() {
        OpenWorker worker = new OpenWorker();
        assertEquals("tbt_open", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("customerId", "CUST-767", "issueType", "no-service"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TKT-trouble-ticket-001", result.getOutputData().get("ticketId"));
    }
}
