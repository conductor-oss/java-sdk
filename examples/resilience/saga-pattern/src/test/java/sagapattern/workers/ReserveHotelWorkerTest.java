package sagapattern.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReserveHotelWorkerTest {

    private final ReserveHotelWorker worker = new ReserveHotelWorker();

    @BeforeEach
    void setUp() {
        BookingStore.clear();
    }

    @Test
    void taskDefName() {
        assertEquals("saga_reserve_hotel", worker.getTaskDefName());
    }

    @Test
    void reservesHotelAndStoresBooking() {
        Task task = taskWith(Map.of("tripId", "TRIP-100"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("HTL-TRIP-100", result.getOutputData().get("reservationId"));
        assertTrue(BookingStore.HOTEL_RESERVATIONS.containsKey("HTL-TRIP-100"),
                "Reservation should be in store");
    }

    @Test
    void reservationIdUsesTripId() {
        Task task = taskWith(Map.of("tripId", "ABC-999"));
        TaskResult result = worker.execute(task);
        assertEquals("HTL-ABC-999", result.getOutputData().get("reservationId"));
    }

    @Test
    void failsOnMissingTripId() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void failsWhenShouldFailIsTrue() {
        Task task = taskWith(Map.of("tripId", "TRIP-100", "shouldFail", true));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertFalse(BookingStore.HOTEL_RESERVATIONS.containsKey("HTL-TRIP-100"),
                "Hotel should NOT be in store on failure");
    }

    @Test
    void recordsActionInLog() {
        Task task = taskWith(Map.of("tripId", "TRIP-100"));
        worker.execute(task);
        assertTrue(BookingStore.getActionLog().contains("RESERVE_HOTEL:HTL-TRIP-100"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
