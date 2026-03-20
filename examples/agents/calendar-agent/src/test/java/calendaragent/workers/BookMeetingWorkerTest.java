package calendaragent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BookMeetingWorkerTest {

    private final BookMeetingWorker worker = new BookMeetingWorker();

    @Test
    void taskDefName() {
        assertEquals("cl_book_meeting", worker.getTaskDefName());
    }

    @Test
    void booksStandardMeeting() {
        Task task = taskWith(Map.of(
                "selectedSlot", Map.of("date", "2026-03-12", "start", "09:00", "end", "10:00"),
                "attendees", List.of(
                        Map.of("name", "Alice", "email", "alice@company.com"),
                        Map.of("name", "Bob", "email", "bob@company.com"),
                        Map.of("name", "Carol", "email", "carol@company.com")),
                "title", "Q1 Architecture Review",
                "duration", 60));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("mtg-fixed-001", result.getOutputData().get("meetingId"));
        assertEquals("confirmed", result.getOutputData().get("status"));
    }

    @Test
    void returnsFixedMeetingId() {
        Task task = taskWith(Map.of(
                "selectedSlot", Map.of("date", "2026-03-12", "start", "09:00", "end", "10:00"),
                "attendees", List.of(Map.of("name", "Alice", "email", "alice@company.com")),
                "title", "Test Meeting",
                "duration", 60));
        TaskResult result = worker.execute(task);

        assertEquals("mtg-fixed-001", result.getOutputData().get("meetingId"));
    }

    @Test
    void returnsCalendarLink() {
        Task task = taskWith(Map.of(
                "selectedSlot", Map.of("date", "2026-03-12", "start", "09:00", "end", "10:00"),
                "attendees", List.of(Map.of("name", "Alice", "email", "alice@company.com")),
                "title", "Test Meeting",
                "duration", 60));
        TaskResult result = worker.execute(task);

        assertEquals("https://calendar.company.com/event/mtg-fixed-001",
                result.getOutputData().get("calendarLink"));
    }

    @Test
    void countsInvitesSent() {
        Task task = taskWith(Map.of(
                "selectedSlot", Map.of("date", "2026-03-12", "start", "09:00", "end", "10:00"),
                "attendees", List.of(
                        Map.of("name", "Alice", "email", "alice@company.com"),
                        Map.of("name", "Bob", "email", "bob@company.com"),
                        Map.of("name", "Carol", "email", "carol@company.com")),
                "title", "Review",
                "duration", 60));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("invitesSent"));
    }

    @Test
    void formatsScheduledTime() {
        Task task = taskWith(Map.of(
                "selectedSlot", Map.of("date", "2026-03-12", "start", "09:00", "end", "10:00"),
                "attendees", List.of(Map.of("name", "Alice", "email", "alice@company.com")),
                "title", "Review",
                "duration", 60));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-12 09:00-10:00 ET", result.getOutputData().get("scheduledTime"));
    }

    @Test
    void handlesMissingTitle() {
        Map<String, Object> input = new HashMap<>();
        input.put("selectedSlot", Map.of("date", "2026-03-12", "start", "09:00", "end", "10:00"));
        input.put("attendees", List.of(Map.of("name", "Alice", "email", "alice@company.com")));
        input.put("title", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("mtg-fixed-001", result.getOutputData().get("meetingId"));
        assertEquals(0, result.getOutputData().get("invitesSent"));
    }

    @Test
    void handlesNullAttendees() {
        Map<String, Object> input = new HashMap<>();
        input.put("selectedSlot", Map.of("date", "2026-03-11", "start", "15:00", "end", "16:00"));
        input.put("attendees", null);
        input.put("title", "Sync");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("invitesSent"));
        assertEquals("2026-03-11 15:00-16:00 ET", result.getOutputData().get("scheduledTime"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
