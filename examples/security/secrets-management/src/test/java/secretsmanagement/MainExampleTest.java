package secretsmanagement;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import secretsmanagement.workers.CreateSecretWorker;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MainExampleTest {
    @Test void createsUniqueSecrets() {
        CreateSecretWorker w = new CreateSecretWorker();
        Task t1 = new Task(); t1.setStatus(Task.Status.IN_PROGRESS);
        t1.setInputData(new HashMap<>(Map.of("secretName", "api-key", "length", 32)));
        Task t2 = new Task(); t2.setStatus(Task.Status.IN_PROGRESS);
        t2.setInputData(new HashMap<>(Map.of("secretName", "api-key", "length", 32)));
        TaskResult r1 = w.execute(t1);
        TaskResult r2 = w.execute(t2);
        assertNotEquals(r1.getOutputData().get("secretValue"), r2.getOutputData().get("secretValue"));
    }
}
