package sagapattern.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BookFlightWorkerTest {

    private final BookFlightWorker worker = new BookFlightWorker();

    @BeforeEach
    void setUp() {
        BookingStore.clear();
    }

    @Test
    void taskDefName() {
        assertEquals("saga_book_flight", worker.getTaskDefName());
    }

    @Test
    void booksFlightAndStoresBooking() {
        Task task = taskWith(Map.of("tripId", "TRIP-100"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("FLT-TRIP-100", result.getOutputData().get("bookingId"));
        assertTrue(BookingStore.FLIGHT_BOOKINGS.containsKey("FLT-TRIP-100"),
                "Booking should be in store");
    }

    @Test
    void bookingIdUsesTripId() {
        Task task = taskWith(Map.of("tripId", "ABC-999"));
        TaskResult result = worker.execute(task);
        assertEquals("FLT-ABC-999", result.getOutputData().get("bookingId"));
    }

    @Test
    void failsOnMissingTripId() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("tripId"));
    }

    @Test
    void failsOnBlankTripId() {
        Task task = taskWith(Map.of("tripId", "  "));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void recordsActionInLog() {
        Task task = taskWith(Map.of("tripId", "TRIP-100"));
        worker.execute(task);
        assertTrue(BookingStore.getActionLog().contains("BOOK_FLIGHT:FLT-TRIP-100"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
