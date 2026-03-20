package ticketmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CreateTicketWorkerTest {
    private final CreateTicketWorker worker = new CreateTicketWorker();
    @Test void taskDefName() { assertEquals("tkt_create", worker.getTaskDefName()); }
    @Test void createsTicket() {
        TaskResult r = worker.execute(taskWith(Map.of("subject", "Test", "description", "desc", "reportedBy", "a@b.com")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("ticketId"));
        assertTrue(((String) r.getOutputData().get("ticketId")).startsWith("TKT-"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
