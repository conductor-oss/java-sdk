package governmentpermit.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateWorkerTest {

    @Test
    void testValidateWorker() {
        ValidateWorker worker = new ValidateWorker();
        assertEquals("gvp_validate", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("application", Map.of("id", "PRM-001", "applicantId", "CIT-100")));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> validatedApp = (Map<String, Object>) result.getOutputData().get("validatedApp");
        assertNotNull(validatedApp);
        assertEquals(true, validatedApp.get("valid"));
    }
}
