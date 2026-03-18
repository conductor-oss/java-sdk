package sagapattern.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CancelHotelWorkerTest {

    private final CancelHotelWorker worker = new CancelHotelWorker();

    @BeforeEach
    void setUp() {
        BookingStore.clear();
    }

    @Test
    void taskDefName() {
        assertEquals("saga_cancel_hotel", worker.getTaskDefName());
    }

    @Test
    void cancelsExistingHotel() {
        BookingStore.HOTEL_RESERVATIONS.put("HTL-TRIP-100", "2024-01-01T00:00:00Z");

        Task task = taskWith(Map.of("tripId", "TRIP-100", "reservationId", "HTL-TRIP-100"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("cancelled"));
        assertEquals(true, result.getOutputData().get("removedFromStore"));
        assertFalse(BookingStore.HOTEL_RESERVATIONS.containsKey("HTL-TRIP-100"));
    }

    @Test
    void handlesNonExistentReservation() {
        Task task = taskWith(Map.of("tripId", "TRIP-999"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("cancelled"));
        assertEquals(false, result.getOutputData().get("removedFromStore"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
