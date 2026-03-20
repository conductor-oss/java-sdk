package governmentpermit.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApplyWorkerTest {

    @Test
    void testApplyWorker() {
        ApplyWorker worker = new ApplyWorker();
        assertEquals("gvp_apply", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("applicantId", "CIT-100", "permitType", "building"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> application = (Map<String, Object>) result.getOutputData().get("application");
        assertNotNull(application);
        assertEquals("CIT-100", application.get("applicantId"));
        assertEquals("building", application.get("permitType"));
    }
}
