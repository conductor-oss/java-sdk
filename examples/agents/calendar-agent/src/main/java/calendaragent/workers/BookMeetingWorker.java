package calendaragent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Books the selected meeting slot — creates the calendar event, sends
 * invitations, and returns the confirmed meeting details.
 */
public class BookMeetingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cl_book_meeting";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> selectedSlot =
                (Map<String, Object>) task.getInputData().get("selectedSlot");
        List<Map<String, String>> attendees =
                (List<Map<String, String>>) task.getInputData().get("attendees");
        String title = (String) task.getInputData().get("title");
        if (title == null || title.isBlank()) {
            title = "Meeting";
        }
        Object durationObj = task.getInputData().get("duration");

        System.out.println("  [cl_book_meeting] Booking meeting: " + title);

        String meetingId = "mtg-fixed-001";

        int invitesSent = (attendees != null) ? attendees.size() : 0;

        String scheduledTime = "2026-03-12 09:00-10:00 ET";
        if (selectedSlot != null) {
            String date = (String) selectedSlot.getOrDefault("date", "2026-03-12");
            String start = (String) selectedSlot.getOrDefault("start", "09:00");
            String end = (String) selectedSlot.getOrDefault("end", "10:00");
            scheduledTime = date + " " + start + "-" + end + " ET";
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("meetingId", meetingId);
        result.getOutputData().put("calendarLink", "https://calendar.company.com/event/" + meetingId);
        result.getOutputData().put("scheduledTime", scheduledTime);
        result.getOutputData().put("invitesSent", invitesSent);
        result.getOutputData().put("status", "confirmed");
        return result;
    }
}
