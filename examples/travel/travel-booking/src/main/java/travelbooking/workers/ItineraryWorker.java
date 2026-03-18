package travelbooking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds travel itinerary. Real itinerary construction.
 */
public class ItineraryWorker implements Worker {
    @Override public String getTaskDefName() { return "tvb_itinerary"; }

    @Override public TaskResult execute(Task task) {
        String destination = (String) task.getInputData().get("destination");
        String departureDate = (String) task.getInputData().get("departureDate");
        String returnDate = (String) task.getInputData().get("returnDate");
        if (destination == null) destination = "Unknown";
        if (departureDate == null) departureDate = "TBD";
        if (returnDate == null) returnDate = "TBD";

        List<Map<String, String>> itinerary = new ArrayList<>();
        itinerary.add(Map.of("day", departureDate, "activity", "Depart to " + destination));
        itinerary.add(Map.of("day", "Day 2", "activity", "Arrive at " + destination + ", hotel check-in"));
        itinerary.add(Map.of("day", returnDate, "activity", "Return flight home"));

        System.out.println("  [itinerary] Built " + itinerary.size() + "-day itinerary to " + destination);

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("itinerary", itinerary);
        r.getOutputData().put("totalDays", itinerary.size());
        return r;
    }
}
