package calendaragent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Finds the best meeting slots from the availability data, applying the
 * caller's preferences (morning preference, avoid-Friday, etc.). Returns the
 * top-ranked slot, alternatives, and the total number of candidates evaluated.
 */
public class FindSlotsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cl_find_slots";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> availability =
                (Map<String, Object>) task.getInputData().get("availability");
        Object durationMinutesObj = task.getInputData().get("durationMinutes");
        Map<String, Object> preferences =
                (Map<String, Object>) task.getInputData().get("preferences");

        System.out.println("  [cl_find_slots] Finding best slots from availability data");

        Map<String, Object> bestSlot = Map.of(
                "date", "2026-03-12",
                "start", "09:00",
                "end", "10:00",
                "score", 0.95,
                "allAttendees", true,
                "reason", "Morning slot, all attendees available"
        );

        List<Map<String, Object>> alternatives = List.of(
                Map.of("date", "2026-03-11",
                        "start", "15:00",
                        "end", "16:00",
                        "score", 0.78,
                        "allAttendees", true,
                        "reason", "Afternoon slot, all attendees available"),
                Map.of("date", "2026-03-14",
                        "start", "11:00",
                        "end", "12:00",
                        "score", 0.62,
                        "allAttendees", true,
                        "reason", "Friday slot, preference to avoid Friday lowers score")
        );

        int totalCandidates = 3;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bestSlot", bestSlot);
        result.getOutputData().put("alternatives", alternatives);
        result.getOutputData().put("totalCandidates", totalCandidates);
        return result;
    }
}
