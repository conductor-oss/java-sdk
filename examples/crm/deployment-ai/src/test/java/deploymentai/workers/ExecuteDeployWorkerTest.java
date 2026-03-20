package deploymentai.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class ExecuteDeployWorkerTest {
    private final ExecuteDeployWorker worker = new ExecuteDeployWorker();
    @Test void taskDefName() { assertEquals("dai_execute_deploy", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("strategy","blue-green"); input.put("serviceName","svc"); input.put("version","1.0"); input.put("environment","prod");
        t.setInputData(input);
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
