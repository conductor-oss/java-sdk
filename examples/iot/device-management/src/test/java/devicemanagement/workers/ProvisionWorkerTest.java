package devicemanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ProvisionWorkerTest {
    private final ProvisionWorker worker = new ProvisionWorker();

    @Test void taskDefName() { assertNotNull(worker.getTaskDefName()); }

    @Test void executesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("deviceId", "DEV-001", "deviceType", "sensor",
                "fleetId", "FLEET-1", "currentFirmware", "1.0.0")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
