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
    void statusIsAlwaysCompleted() {
        Task task = taskWith(Map.of("tripId", "TRIP-X"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
