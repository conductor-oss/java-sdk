package apigateway.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AgAuthenticateWorkerTest {
    @Test void authenticatesApiKey() {
        AgAuthenticateWorker w = new AgAuthenticateWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("apiKey", "sk-test-123")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("clientId"));
        assertNotNull(r.getOutputData().get("tier"));
        assertTrue(((Number) r.getOutputData().get("rateLimit")).intValue() > 0);
    }
}
