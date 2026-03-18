package bluegreendeploy.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VerifyDeploymentWorkerTest {
    @Test void executesSuccessfully() {
        VerifyDeploymentWorker w = new VerifyDeploymentWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("appName", "my-service", "newVersion", "2.0.0",
                "from", "blue", "to", "green", "version", "2.0.0", "activeEnv", "green")));
        TaskResult r = w.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
