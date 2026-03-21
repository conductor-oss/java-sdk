package sagaforkjoin.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckResultsWorkerTest {

    private final CheckResultsWorker worker = new CheckResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("sfj_check_results", worker.getTaskDefName());
    }

    @Test
    void allValidWhenAllBookingsPresent() {
        Task task = taskWith(Map.of(
                "hotelBookingId", "HTL-001",
                "flightBookingId", "FLT-001",
                "carBookingId", "CAR-001"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("allValid"));
        assertEquals("HTL-001", result.getOutputData().get("hotelBookingId"));
        assertEquals("FLT-001", result.getOutputData().get("flightBookingId"));
        assertEquals("CAR-001", result.getOutputData().get("carBookingId"));
    }

    @Test
    void failsWhenHotelBookingMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "flightBookingId", "FLT-001",
                "carBookingId", "CAR-001"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("allValid"));
        assertNotNull(result.getOutputData().get("error"));
    }

    @Test
    void failsWhenFlightBookingMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "hotelBookingId", "HTL-001",
                "carBookingId", "CAR-001"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("allValid"));
    }

    @Test
    void failsWhenCarBookingMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "hotelBookingId", "HTL-001",
                "flightBookingId", "FLT-001"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("allValid"));
    }

    @Test
    void failsWhenAllBookingsMissing() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("allValid"));
        assertNotNull(result.getOutputData().get("error"));
    }

    @Test
    void failsWhenBookingIdIsEmpty() {
        Map<String, Object> input = new HashMap<>();
        input.put("hotelBookingId", "");
        input.put("flightBookingId", "FLT-001");
        input.put("carBookingId", "CAR-001");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("allValid"));
    }

    @Test
    void outputContainsAllBookingIds() {
        Task task = taskWith(Map.of(
                "hotelBookingId", "HTL-001",
                "flightBookingId", "FLT-001",
                "carBookingId", "CAR-001"
        ));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("hotelBookingId"));
        assertTrue(result.getOutputData().containsKey("flightBookingId"));
        assertTrue(result.getOutputData().containsKey("carBookingId"));
        assertTrue(result.getOutputData().containsKey("allValid"));
    }

    @Test
    void handlesNullBookingIdValue() {
        Map<String, Object> input = new HashMap<>();
        input.put("hotelBookingId", null);
        input.put("flightBookingId", "FLT-001");
        input.put("carBookingId", "CAR-001");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals(false, result.getOutputData().get("allValid"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
