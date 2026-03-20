package calendaragent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Checks calendar availability for all attendees across the specified date
 * range. Returns per-date time slots with available attendee emails, a list
 * of busy days, and a count of fully-free days.
 */
public class CheckCalendarWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cl_check_calendar";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, String>> attendees =
                (List<Map<String, String>>) task.getInputData().get("attendees");
        Map<String, String> dateRange =
                (Map<String, String>) task.getInputData().get("dateRange");
        Object durationObj = task.getInputData().get("duration");

        System.out.println("  [cl_check_calendar] Checking calendars for "
                + (attendees != null ? attendees.size() : 0) + " attendees");

        // deterministic availability data across the date range
        Map<String, List<Map<String, Object>>> availability = Map.of(
                "2026-03-10", List.of(
                        Map.of("start", "09:00", "end", "10:00",
                                "available", List.of("alice@company.com", "bob@company.com")),
                        Map.of("start", "14:00", "end", "15:00",
                                "available", List.of("alice@company.com", "carol@company.com"))
                ),
                "2026-03-11", List.of(
                        Map.of("start", "10:00", "end", "11:00",
                                "available", List.of("bob@company.com", "carol@company.com")),
                        Map.of("start", "15:00", "end", "16:00",
                                "available", List.of("alice@company.com", "bob@company.com", "carol@company.com"))
                ),
                "2026-03-12", List.of(
                        Map.of("start", "09:00", "end", "10:00",
                                "available", List.of("alice@company.com", "bob@company.com", "carol@company.com")),
                        Map.of("start", "13:00", "end", "14:00",
                                "available", List.of("alice@company.com", "carol@company.com"))
                ),
                "2026-03-13", List.of(),
                "2026-03-14", List.of(
                        Map.of("start", "11:00", "end", "12:00",
                                "available", List.of("alice@company.com", "bob@company.com", "carol@company.com"))
                )
        );

        List<String> busyDays = List.of("2026-03-13");
        int fullyFree = 0;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("availability", availability);
        result.getOutputData().put("busyDays", busyDays);
        result.getOutputData().put("fullyFree", fullyFree);
        return result;
    }
}
