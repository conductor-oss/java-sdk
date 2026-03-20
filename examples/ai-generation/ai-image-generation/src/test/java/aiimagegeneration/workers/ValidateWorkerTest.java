package aiimagegeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ValidateWorkerTest {

    @Test
    void testValidateWorker() {
        ValidateWorker worker = new ValidateWorker();
        assertEquals("aig_validate", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("imageId", "IMG-TEST"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.94, result.getOutputData().get("qualityScore"));
        assertEquals(true, result.getOutputData().get("safeContent"));
    }
}
