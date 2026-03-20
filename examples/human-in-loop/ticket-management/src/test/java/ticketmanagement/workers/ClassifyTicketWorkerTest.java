package ticketmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ClassifyTicketWorkerTest {
    private final ClassifyTicketWorker worker = new ClassifyTicketWorker();
    @Test void taskDefName() { assertEquals("tkt_classify", worker.getTaskDefName()); }
    @Test void classifiesAuthentication() {
        TaskResult r = worker.execute(taskWith(Map.of("ticketId", "TKT-1", "description", "Cannot login to dashboard")));
        assertEquals("authentication", r.getOutputData().get("category"));
        assertEquals("P1", r.getOutputData().get("priority"));
    }
    @Test void classifiesPerformance() {
        TaskResult r = worker.execute(taskWith(Map.of("ticketId", "TKT-2", "description", "Page loads are slow")));
        assertEquals("performance", r.getOutputData().get("category"));
    }
    @Test void classifiesGeneral() {
        TaskResult r = worker.execute(taskWith(Map.of("ticketId", "TKT-3", "description", "How do I export data?")));
        assertEquals("general", r.getOutputData().get("category"));
        assertEquals("P2", r.getOutputData().get("priority"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
