package roamingmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DetectRoamingWorkerTest {

    @Test
    void testDetectRoamingWorker() {
        DetectRoamingWorker worker = new DetectRoamingWorker();
        assertEquals("rmg_detect_roaming", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("subscriberId", "SUB-820", "visitedNetwork", "DE-Telco"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("DE", result.getOutputData().get("country"));
    }
}
