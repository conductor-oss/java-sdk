package agencymanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OnboardWorkerTest {

    @Test
    void testOnboardWorker() {
        OnboardWorker worker = new OnboardWorker();
        assertEquals("agm_onboard", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("agentId", "AGT-702", "agentName", "Sarah Chen"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("onboarded"));
    }
}
