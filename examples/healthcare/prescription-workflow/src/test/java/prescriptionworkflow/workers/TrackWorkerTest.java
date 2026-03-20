package prescriptionworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TrackWorkerTest {

    private final TrackWorker worker = new TrackWorker();

    @Test
    void taskDefName() {
        assertEquals("prx_track", worker.getTaskDefName());
    }

    @Test
    void tracksWithValidPrescriptionId() {
        Task task = taskWith("RX-12345");

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TRK-PRX-44201", result.getOutputData().get("trackingId"));
        assertNotNull(result.getOutputData().get("refillDate"));
        assertEquals(true, result.getOutputData().get("adherenceMonitoring"));
    }

    @Test
    void handlesNullPrescriptionId() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("trackingId"));
    }

    @Test
    void refillDateIsInFuture() {
        Task task = taskWith("RX-99999");

        TaskResult result = worker.execute(task);

        String refillDate = (String) result.getOutputData().get("refillDate");
        assertNotNull(refillDate);
        assertTrue(refillDate.matches("\\d{4}-\\d{2}-\\d{2}"), "refillDate should be YYYY-MM-DD");
    }

    private Task taskWith(String prescriptionId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("prescriptionId", prescriptionId);
        task.setInputData(input);
        return task;
    }
}
