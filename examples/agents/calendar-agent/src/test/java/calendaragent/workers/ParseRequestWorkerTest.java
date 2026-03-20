package calendaragent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseRequestWorkerTest {

    private final ParseRequestWorker worker = new ParseRequestWorker();

    @Test
    void taskDefName() {
        assertEquals("cl_parse_request", worker.getTaskDefName());
    }

    @Test
    void parsesStandardRequest() {
        Task task = taskWith(Map.of(
                "request", "Schedule a Q1 architecture review",
                "attendees", List.of("alice@company.com", "bob@company.com", "carol@company.com"),
                "duration", "1 hour",
                "preferredDate", "next week"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Q1 Architecture Review", result.getOutputData().get("meetingTitle"));
        assertEquals(60, result.getOutputData().get("durationMinutes"));
    }

    @Test
    void returnsParsedAttendees() {
        Task task = taskWith(Map.of(
                "request", "Schedule meeting",
                "attendees", List.of("alice@company.com", "bob@company.com", "carol@company.com"),
                "duration", "1 hour",
                "preferredDate", "next week"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> parsedAttendees =
                (List<Map<String, String>>) result.getOutputData().get("parsedAttendees");
        assertNotNull(parsedAttendees);
        assertEquals(3, parsedAttendees.size());
        assertTrue(parsedAttendees.get(0).containsKey("name"));
        assertTrue(parsedAttendees.get(0).containsKey("email"));
        assertTrue(parsedAttendees.get(0).containsKey("timezone"));
    }

    @Test
    void returnsDateRange() {
        Task task = taskWith(Map.of(
                "request", "Schedule meeting",
                "attendees", List.of("alice@company.com"),
                "duration", "1 hour",
                "preferredDate", "next week"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, String> dateRange = (Map<String, String>) result.getOutputData().get("dateRange");
        assertNotNull(dateRange);
        assertEquals("2026-03-10", dateRange.get("start"));
        assertEquals("2026-03-14", dateRange.get("end"));
    }

    @Test
    void returnsPreferences() {
        Task task = taskWith(Map.of(
                "request", "Schedule meeting",
                "attendees", List.of("alice@company.com"),
                "duration", "1 hour",
                "preferredDate", "next week"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> preferences = (Map<String, Object>) result.getOutputData().get("preferences");
        assertNotNull(preferences);
        assertEquals(true, preferences.get("preferMorning"));
        assertEquals(true, preferences.get("avoidFriday"));
        assertEquals("America/New_York", preferences.get("timezone"));
    }

    @Test
    void handles30MinuteDuration() {
        Task task = taskWith(Map.of(
                "request", "Quick sync",
                "attendees", List.of("alice@company.com"),
                "duration", "30 minutes",
                "preferredDate", "next week"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(30, result.getOutputData().get("durationMinutes"));
    }

    @Test
    void handlesEmptyRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("request", "");
        input.put("attendees", List.of("alice@company.com"));
        input.put("duration", "1 hour");
        input.put("preferredDate", "next week");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("meetingTitle"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(60, result.getOutputData().get("durationMinutes"));
        assertNotNull(result.getOutputData().get("parsedAttendees"));
    }

    @Test
    void handlesNullRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("request", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("meetingTitle"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
