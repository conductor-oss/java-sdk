package ticketmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CloseTicketWorkerTest {
    private final CloseTicketWorker worker = new CloseTicketWorker();
    @Test void taskDefName() { assertEquals("tkt_close", worker.getTaskDefName()); }
    @Test void closesTicket() {
        TaskResult r = worker.execute(taskWith(Map.of("ticketId", "TKT-1", "resolution", "Fixed")));
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("closed"));
    }
    private Task taskWith(Map<String, Object> input) { Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t; }
}
