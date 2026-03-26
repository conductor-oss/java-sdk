package centralizedconfigmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CfgStageRolloutWorkerTest {
    private final CfgStageRolloutWorker worker = new CfgStageRolloutWorker();
    @Test void taskDefName() { assertEquals("cfg_stage_rollout", worker.getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("configKey", "max")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertNotNull(r.getOutputData().get("plan"));
    }
}
