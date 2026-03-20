package abtesting.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class DefineVariantsWorkerTest {
    private final DefineVariantsWorker worker = new DefineVariantsWorker();
    @Test void taskDefName() { assertEquals("abt_define_variants", worker.getTaskDefName()); }
    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("id", "test-1", "variantA", "Control", "variantB", "Treatment")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("variants"));
        assertEquals("A", r.getOutputData().get("id"));
        assertNotNull(r.getOutputData().get("name"));
    }
}
