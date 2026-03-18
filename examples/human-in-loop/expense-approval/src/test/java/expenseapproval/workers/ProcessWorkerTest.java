package expenseapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ProcessWorkerTest {
    @Test void taskDefName() { assertEquals("exp_process", new ProcessWorker().getTaskDefName()); }

    @Test void processesExpense() {
        ProcessWorker w = new ProcessWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("amount", 250, "category", "office_supplies")));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("processed"));
        assertNotNull(r.getOutputData().get("transactionId"));
    }
}
