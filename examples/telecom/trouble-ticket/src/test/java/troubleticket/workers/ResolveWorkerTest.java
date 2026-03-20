package troubleticket.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ResolveWorkerTest {

    @Test
    void testResolveWorker() {
        ResolveWorker worker = new ResolveWorker();
        assertEquals("tbt_resolve", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("ticketId", "TKT-trouble-ticket-001", "assignee", "FT-22"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("cable-replaced", result.getOutputData().get("resolution"));
    }
}
