package emergencyresponse.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectWorkerTest {

    @Test
    void testDetectWorker() {
        DetectWorker worker = new DetectWorker();
        assertEquals("emr_detect", worker.getTaskDefName());

        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("incidentType", "structure-fire", "location", "123 Oak Ave"));

        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("INC-emergency-response-001", result.getOutputData().get("incidentId"));
    }
}
