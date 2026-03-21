package ticketmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AssignTicketWorkerTest {
    private final AssignTicketWorker worker = new AssignTicketWorker();
    @Test void taskDefName() { assertEquals("tkt_assign", worker.getTaskDefName()); }
    @Test void assignsToAuthTeam() {
        TaskResult r = worker.execute(taskWith(Map.of("ticketId", "TKT-1", "category", "authentication", "priority", "P1")));
        assertEquals("Auth Team - Kim", r.getOutputData().get("assignee"));
        assertEquals(4, r.getOutputData().get("slaHours"));
    }
    @Test void assignsToInfraTeam() {
        TaskResult r = worker.execute(taskWith(Map.of("ticketId", "TKT-2", "category", "performance", "priority", "P2")));
        assertEquals("Infra Team - Leo", r.getOutputData().get("assignee"));
        assertEquals(24, r.getOutputData().get("slaHours"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
