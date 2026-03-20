package sagaforkjoin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BookFlightWorkerTest {

    private final BookFlightWorker worker = new BookFlightWorker();

    @Test
    void taskDefName() {
        assertEquals("sfj_book_flight", worker.getTaskDefName());
    }

    @Test
    void returnsFlightBookingId() {
        Task task = taskWith(Map.of("tripId", "TRIP-42"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("FLT-001", result.getOutputData().get("bookingId"));
        assertEquals("flight", result.getOutputData().get("type"));
        assertEquals("TRIP-42", result.getOutputData().get("tripId"));
    }

    @Test
    void returnsConsistentBookingId() {
        Task task1 = taskWith(Map.of("tripId", "TRIP-1"));
        Task task2 = taskWith(Map.of("tripId", "TRIP-2"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals("FLT-001", result1.getOutputData().get("bookingId"));
        assertEquals("FLT-001", result2.getOutputData().get("bookingId"));
    }

    @Test
    void handlesNullTripId() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("FLT-001", result.getOutputData().get("bookingId"));
        assertEquals("unknown", result.getOutputData().get("tripId"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("tripId", "TRIP-99"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("bookingId"));
        assertTrue(result.getOutputData().containsKey("type"));
        assertTrue(result.getOutputData().containsKey("tripId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
