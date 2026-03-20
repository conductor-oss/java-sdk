package backgroundcheck.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ReportWorkerTest {
    private final ReportWorker w = new ReportWorker();
    @Test void taskDefName() { assertEquals("bgc_report", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("candidateName","Alex","candidateId","CAND-710","criminal","clear","employment","verified","education","verified")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
