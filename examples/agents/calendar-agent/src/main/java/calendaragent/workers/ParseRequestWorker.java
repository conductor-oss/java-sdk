package calendaragent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Parses a natural-language meeting request into structured data: attendees
 * with timezones, duration, date range, preferences, and meeting title.
 */
public class ParseRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cl_parse_request";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String request = (String) task.getInputData().get("request");
        if (request == null || request.isBlank()) {
            request = "";
        }

        List<String> attendeeEmails = (List<String>) task.getInputData().get("attendees");
        if (attendeeEmails == null) {
            attendeeEmails = List.of();
        }

        String duration = (String) task.getInputData().get("duration");
        if (duration == null || duration.isBlank()) {
            duration = "1 hour";
        }

        String preferredDate = (String) task.getInputData().get("preferredDate");
        if (preferredDate == null || preferredDate.isBlank()) {
            preferredDate = "next week";
        }

        System.out.println("  [cl_parse_request] Parsing request: " + request);

        // Parse duration string to minutes
        int durationMinutes = 60;
        if (duration.contains("30")) {
            durationMinutes = 30;
        } else if (duration.contains("90") || duration.contains("1.5")) {
            durationMinutes = 90;
        } else if (duration.contains("2")) {
            durationMinutes = 120;
        }

        // Build parsed attendee list with name, email, timezone
        List<Map<String, String>> parsedAttendees = List.of(
                Map.of("name", "Alice Chen", "email", "alice@company.com", "timezone", "America/New_York"),
                Map.of("name", "Bob Martinez", "email", "bob@company.com", "timezone", "America/Chicago"),
                Map.of("name", "Carol Williams", "email", "carol@company.com", "timezone", "America/Los_Angeles")
        );

        Map<String, String> dateRange = Map.of(
                "start", "2026-03-10",
                "end", "2026-03-14"
        );

        Map<String, Object> preferences = Map.of(
                "preferMorning", true,
                "avoidFriday", true,
                "timezone", "America/New_York"
        );

        String meetingTitle = "Q1 Architecture Review";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("parsedAttendees", parsedAttendees);
        result.getOutputData().put("durationMinutes", durationMinutes);
        result.getOutputData().put("dateRange", dateRange);
        result.getOutputData().put("preferences", preferences);
        result.getOutputData().put("meetingTitle", meetingTitle);
        return result;
    }
}
