package travelapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Estimates travel cost. Real cost breakdown computation.
 */
public class EstimateWorker implements Worker {
    private static final Map<String, Double> FLIGHT_COSTS = Map.of(
            "tokyo", 1200.0, "london", 800.0, "paris", 850.0, "new york", 400.0, "sydney", 1500.0);

    @Override public String getTaskDefName() { return "tva_estimate"; }

    @Override public TaskResult execute(Task task) {
        String destination = (String) task.getInputData().get("destination");
        Object daysObj = task.getInputData().get("days");
        if (destination == null) destination = "unknown";
        int days = daysObj instanceof Number ? ((Number) daysObj).intValue() : 3;

        double flight = FLIGHT_COSTS.getOrDefault(destination.toLowerCase(), 600.0);
        double hotel = days * 200;
        double meals = days * 75;
        double total = flight + hotel + meals;

        System.out.println("  [estimate] " + destination + " " + days + " days: $" + (int) total
                + " (flight: $" + (int) flight + ", hotel: $" + (int) hotel + ", meals: $" + (int) meals + ")");

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("estimatedCost", total);
        r.getOutputData().put("flightCost", flight);
        r.getOutputData().put("hotelCost", hotel);
        r.getOutputData().put("mealsCost", meals);
        return r;
    }
}
