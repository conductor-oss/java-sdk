package budgetapproval.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class AllocateFundsWorkerTest {
    private final AllocateFundsWorker worker = new AllocateFundsWorker();
    @Test void taskDefName() { assertEquals("bgt_allocate_funds", worker.getTaskDefName()); }
    @Test void allocatesOnApprove() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("decision", "approve", "department", "eng")));
        TaskResult r = worker.execute(t);
        assertEquals("allocated", r.getOutputData().get("allocationStatus"));
    }
    @Test void noAllocationOnReject() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("decision", "reject", "department", "eng")));
        TaskResult r = worker.execute(t);
        assertEquals("not_allocated", r.getOutputData().get("allocationStatus"));
    }
}
