package sagaforkjoin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmAllWorkerTest {

    private final ConfirmAllWorker worker = new ConfirmAllWorker();

    @Test
    void taskDefName() {
        assertEquals("sfj_confirm_all", worker.getTaskDefName());
    }

    @Test
    void confirmsWhenAllValid() {
        Task task = taskWith(Map.of(
                "tripId", "TRIP-42",
                "hotelBookingId", "HTL-001",
                "flightBookingId", "FLT-001",
                "carBookingId", "CAR-001",
                "allValid", true
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("confirmed"));
        assertEquals("TRIP-42", result.getOutputData().get("tripId"));
        assertNotNull(result.getOutputData().get("message"));
        assertTrue(result.getOutputData().get("message").toString().contains("confirmed"));
    }

    @Test
    void failsWhenNotAllValid() {
        Task task = taskWith(Map.of(
                "tripId", "TRIP-42",
                "allValid", false
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("confirmed"));
        assertNotNull(result.getOutputData().get("message"));
        assertTrue(result.getOutputData().get("message").toString().contains("rolled back"));
    }

    @Test
    void failsWhenAllValidMissing() {
        Task task = taskWith(new HashMap<>(Map.of("tripId", "TRIP-42")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("confirmed"));
    }

    @Test
    void handlesStringTrueForAllValid() {
        Task task = taskWith(Map.of(
                "tripId", "TRIP-42",
                "allValid", "true"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("confirmed"));
    }

    @Test
    void handlesStringFalseForAllValid() {
        Task task = taskWith(Map.of(
                "tripId", "TRIP-42",
                "allValid", "false"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("confirmed"));
    }

    @Test
    void handlesMissingTripId() {
        Task task = taskWith(new HashMap<>(Map.of("allValid", true)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("confirmed"));
        assertEquals("unknown", result.getOutputData().get("tripId"));
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of(
                "tripId", "TRIP-42",
                "allValid", true
        ));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("confirmed"));
        assertTrue(result.getOutputData().containsKey("tripId"));
        assertTrue(result.getOutputData().containsKey("message"));
    }

    @Test
    void messageIncludesTripId() {
        Task task = taskWith(Map.of(
                "tripId", "TRIP-99",
                "allValid", true
        ));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().get("message").toString().contains("TRIP-99"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
