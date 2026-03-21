package aivoicecloning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VerifyWorkerTest {

    @Test
    void testVerifyWorker() {
        VerifyWorker worker = new VerifyWorker();
        assertEquals("avc_verify", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("audioId", "AUD-TEST", "speakerId", "SPK-TEST"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.96, result.getOutputData().get("similarity"));
        assertEquals(true, result.getOutputData().get("verified"));
    }
}
