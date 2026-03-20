package financialaudit.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class DefineScopeWorkerTest {
    private final DefineScopeWorker worker = new DefineScopeWorker();
    @Test void taskDefName() { assertEquals("fau_define_scope", worker.getTaskDefName()); }
    @Test void definesScope() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("auditId","A-1","entityName","Acme","auditType","annual","fiscalYear",2025)));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("scopeAreas"));
        assertEquals(50000, r.getOutputData().get("materialityThreshold"));
    }
}
