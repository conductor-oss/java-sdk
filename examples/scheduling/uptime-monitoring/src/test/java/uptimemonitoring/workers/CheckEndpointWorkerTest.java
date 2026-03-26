package uptimemonitoring.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CheckEndpointWorkerTest {
    @Test void taskDefName() { assertEquals("um_check_endpoint", new CheckEndpointWorker().getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("endpoint","https://api.example.com", "expectedStatus",200)));
        TaskResult r = new CheckEndpointWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(200, r.getOutputData().get("httpStatus"));
        assertEquals(true, r.getOutputData().get("isUp"));
    }
}
