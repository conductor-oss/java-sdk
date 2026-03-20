package ticketmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ResolveTicketWorkerTest {
    private final ResolveTicketWorker worker = new ResolveTicketWorker();
    @Test void taskDefName() { assertEquals("tkt_resolve", worker.getTaskDefName()); }
    @Test void resolvesTicket() {
        TaskResult r = worker.execute(taskWith(Map.of("ticketId", "TKT-1", "assignee", "Kim")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("resolution"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
