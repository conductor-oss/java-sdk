package sagapattern.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CancelFlightWorkerTest {

    private final CancelFlightWorker worker = new CancelFlightWorker();

    @BeforeEach
    void setUp() {
        BookingStore.clear();
    }

    @Test
    void taskDefName() {
        assertEquals("saga_cancel_flight", worker.getTaskDefName());
    }

    @Test
    void cancelsExistingFlight() {
        BookingStore.FLIGHT_BOOKINGS.put("FLT-TRIP-100", "2024-01-01T00:00:00Z");

        Task task = taskWith(Map.of("tripId", "TRIP-100", "bookingId", "FLT-TRIP-100"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("cancelled"));
        assertEquals(true, result.getOutputData().get("removedFromStore"));
        assertFalse(BookingStore.FLIGHT_BOOKINGS.containsKey("FLT-TRIP-100"));
    }

    @Test
    void handlesNonExistentBooking() {
        Task task = taskWith(Map.of("tripId", "TRIP-999"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("cancelled"));
        assertEquals(false, result.getOutputData().get("removedFromStore"));
    }

    @Test
    void failsOnMissingTripId() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void recordsActionInLog() {
        Task task = taskWith(Map.of("tripId", "TRIP-100"));
        worker.execute(task);
        assertTrue(BookingStore.getActionLog().contains("CANCEL_FLIGHT:FLT-TRIP-100"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
