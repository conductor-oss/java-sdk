package calendaragent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckCalendarWorkerTest {

    private final CheckCalendarWorker worker = new CheckCalendarWorker();

    @Test
    void taskDefName() {
        assertEquals("cl_check_calendar", worker.getTaskDefName());
    }

    @Test
    void returnsAvailability() {
        Task task = taskWith(Map.of(
                "attendees", List.of(
                        Map.of("name", "Alice", "email", "alice@company.com", "timezone", "America/New_York"),
                        Map.of("name", "Bob", "email", "bob@company.com", "timezone", "America/Chicago"),
                        Map.of("name", "Carol", "email", "carol@company.com", "timezone", "America/Los_Angeles")),
                "dateRange", Map.of("start", "2026-03-10", "end", "2026-03-14"),
                "duration", 60));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("availability"));
    }

    @Test
    void availabilityContainsExpectedDates() {
        Task task = taskWith(Map.of(
                "attendees", List.of(Map.of("name", "Alice", "email", "alice@company.com")),
                "dateRange", Map.of("start", "2026-03-10", "end", "2026-03-14"),
                "duration", 60));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> availability = (Map<String, Object>) result.getOutputData().get("availability");
        assertTrue(availability.containsKey("2026-03-10"));
        assertTrue(availability.containsKey("2026-03-12"));
        assertTrue(availability.containsKey("2026-03-13"));
    }

    @Test
    void returnsBusyDays() {
        Task task = taskWith(Map.of(
                "attendees", List.of(Map.of("name", "Alice", "email", "alice@company.com")),
                "dateRange", Map.of("start", "2026-03-10", "end", "2026-03-14"),
                "duration", 60));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> busyDays = (List<String>) result.getOutputData().get("busyDays");
        assertNotNull(busyDays);
        assertTrue(busyDays.contains("2026-03-13"));
    }

    @Test
    void returnsFullyFreeCount() {
        Task task = taskWith(Map.of(
                "attendees", List.of(Map.of("name", "Alice", "email", "alice@company.com")),
                "dateRange", Map.of("start", "2026-03-10", "end", "2026-03-14"),
                "duration", 60));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("fullyFree"));
    }

    @Test
    void busyDayHasNoSlots() {
        Task task = taskWith(Map.of(
                "attendees", List.of(Map.of("name", "Alice", "email", "alice@company.com")),
                "dateRange", Map.of("start", "2026-03-10", "end", "2026-03-14"),
                "duration", 60));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, Object>>> availability =
                (Map<String, List<Map<String, Object>>>) result.getOutputData().get("availability");
        List<Map<String, Object>> busyDaySlots = availability.get("2026-03-13");
        assertNotNull(busyDaySlots);
        assertTrue(busyDaySlots.isEmpty());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("availability"));
    }

    @Test
    void handlesNullAttendees() {
        Map<String, Object> input = new HashMap<>();
        input.put("attendees", null);
        input.put("dateRange", Map.of("start", "2026-03-10", "end", "2026-03-14"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void slotsContainAvailableEmails() {
        Task task = taskWith(Map.of(
                "attendees", List.of(Map.of("name", "Alice", "email", "alice@company.com")),
                "dateRange", Map.of("start", "2026-03-10", "end", "2026-03-14"),
                "duration", 60));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, Object>>> availability =
                (Map<String, List<Map<String, Object>>>) result.getOutputData().get("availability");
        List<Map<String, Object>> march12Slots = availability.get("2026-03-12");
        assertFalse(march12Slots.isEmpty());
        assertNotNull(march12Slots.get(0).get("available"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
