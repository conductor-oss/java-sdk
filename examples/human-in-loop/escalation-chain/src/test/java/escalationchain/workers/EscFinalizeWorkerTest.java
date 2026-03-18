package escalationchain.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EscFinalizeWorkerTest {
    @Test void taskDefName() { assertEquals("esc_finalize", new EscFinalizeWorker().getTaskDefName()); }

    @Test void finalizesDecision() {
        EscFinalizeWorker w = new EscFinalizeWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("decision", "approved", "level", "Manager")));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("done"));
        assertEquals(true, r.getOutputData().get("approved"));
    }
}
