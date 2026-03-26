package publichealth.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectOutbreakWorkerTest {

    @Test
    void testOutbreakDetected() {
        DetectOutbreakWorker worker = new DetectOutbreakWorker();
        assertEquals("phw_detect_outbreak", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("caseCount", 45, "baseline", 15));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("alert", result.getOutputData().get("action"));
        assertEquals("high", result.getOutputData().get("severity"));
    }

    @Test
    void testNormalLevels() {
        DetectOutbreakWorker worker = new DetectOutbreakWorker();

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("caseCount", 10, "baseline", 15));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("monitor", result.getOutputData().get("action"));
        assertEquals("low", result.getOutputData().get("severity"));
    }
}
